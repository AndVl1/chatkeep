package ru.andvl.chatkeep.domain.service.twitch

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.requests.edit.caption.EditChatMessageCaption
import dev.inmo.tgbotapi.requests.edit.text.EditChatMessageText
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.requests.send.media.SendPhoto
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.row
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.domain.model.twitch.StreamTimelineEvent
import ru.andvl.chatkeep.domain.model.twitch.TwitchNotificationSettings
import ru.andvl.chatkeep.domain.model.twitch.TwitchStream
import ru.andvl.chatkeep.infrastructure.repository.twitch.StreamTimelineEventRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchNotificationSettingsRepository
import java.time.Duration
import java.time.Instant

@Service
class TwitchNotificationService(
    private val bot: TelegramBot,
    private val settingsRepository: TwitchNotificationSettingsRepository,
    private val timelineRepository: StreamTimelineEventRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Send stream start notification
     */
    suspend fun sendStreamStartNotification(
        chatId: Long,
        stream: TwitchStream,
        streamerName: String,
        streamerLogin: String,
        thumbnailUrl: String?
    ): Long? {
        try {
            val settings = settingsRepository.findByChatId(chatId)
                ?: TwitchNotificationSettings.createNew(chatId)

            val caption = formatStreamCaption(
                settings = settings,
                stream = stream,
                streamerName = streamerName,
                timeline = emptyList()
            )

            val keyboard = InlineKeyboardMarkup(
                keyboard = listOf(
                    row {
                        add(URLInlineKeyboardButton(settings.buttonText, "https://twitch.tv/$streamerLogin"))
                    }
                )
            )

            // Get thumbnail (replace template variables)
            val thumbnail = thumbnailUrl
                ?.replace("{width}", "1280")
                ?.replace("{height}", "720")

            val message = if (thumbnail != null) {
                // Send photo with caption
                bot.execute(
                    SendPhoto(
                        chatId = chatId.toChatId(),
                        photo = InputFile.fromUrl(thumbnail),
                        text = caption,
                        replyMarkup = keyboard
                    )
                )
            } else {
                // No thumbnail available (e.g., stream just started) - send text message
                logger.warn("No thumbnail available for stream ${stream.id}, sending text-only notification")
                bot.execute(
                    SendTextMessage(
                        chatId = chatId.toChatId(),
                        text = caption,
                        replyMarkup = keyboard
                    )
                )
            }

            logger.info("Sent stream start notification: chatId=$chatId, messageId=${message.messageId}, stream=${stream.id}, hasPhoto=${thumbnail != null}")
            return message.messageId.long
        } catch (e: Exception) {
            logger.error("Failed to send stream start notification: chatId=$chatId", e)
            return null
        }
    }

    /**
     * Update stream message with new info
     */
    suspend fun updateStreamNotification(
        chatId: Long,
        messageId: Long,
        stream: TwitchStream,
        streamerName: String,
        streamerLogin: String,
        timeline: List<StreamTimelineEvent>
    ) {
        try {
            val settings = settingsRepository.findByChatId(chatId)
                ?: TwitchNotificationSettings.createNew(chatId)

            val caption = formatStreamCaption(
                settings = settings,
                stream = stream,
                streamerName = streamerName,
                timeline = timeline
            )

            // Build keyboard with stream link button
            val keyboard = InlineKeyboardMarkup(
                keyboard = listOf(
                    row {
                        add(URLInlineKeyboardButton(settings.buttonText, "https://twitch.tv/$streamerLogin"))
                    }
                )
            )

            // Try to edit as caption first (for photo messages)
            try {
                bot.execute(
                    EditChatMessageCaption(
                        chatId = chatId.toChatId(),
                        messageId = MessageId(messageId),
                        text = caption,
                        replyMarkup = keyboard
                    )
                )
            } catch (captionError: Exception) {
                // If editing caption fails, try editing as text message
                logger.info("EditChatMessageCaption failed, trying EditChatMessageText: ${captionError.message}")
                bot.execute(
                    EditChatMessageText(
                        chatId = chatId.toChatId(),
                        messageId = MessageId(messageId),
                        text = caption,
                        replyMarkup = keyboard
                    )
                )
            }

            logger.info("Updated stream notification: chatId=$chatId, messageId=$messageId")
        } catch (e: Exception) {
            logger.error("Failed to update stream notification: chatId=$chatId, messageId=$messageId", e)
        }
    }

    /**
     * Format stream caption using template
     */
    private fun formatStreamCaption(
        settings: TwitchNotificationSettings,
        stream: TwitchStream,
        streamerName: String,
        timeline: List<StreamTimelineEvent>
    ): String {
        val isEnded = stream.status == "ended"

        // Calculate duration
        val endTime = stream.endedAt ?: Instant.now()
        val duration = formatDuration(Duration.between(stream.startedAt, endTime))

        // Choose template based on stream status
        val template = if (isEnded) {
            settings.endedMessageTemplate
        } else {
            settings.messageTemplate
        }

        logger.info("formatStreamCaption: isEnded=$isEnded, timelineSize=${timeline.size}, template=${template.take(50)}...")

        var caption = template
            .replace("{streamer}", streamerName)
            .replace("{title}", stream.currentTitle ?: "")
            .replace("{game}", stream.currentGame ?: "Just Chatting")
            .replace("{viewers}", stream.viewerCount.toString())
            .replace("{duration}", duration)

        // Add timeline for ended streams or if there are multiple events
        if (timeline.isNotEmpty() && (isEnded || timeline.size > 1)) {
            val timelineStr = timeline.joinToString("\n") { event ->
                val time = formatSeconds(event.streamOffsetSeconds)
                val game = event.gameName ?: "Just Chatting"
                val title = event.streamTitle ?: ""
                "$time - $game | $title"
            }
            caption += "\n\n📋 Таймлайн:\n$timelineStr"
        }

        return caption
    }

    /**
     * Format duration as HH:MM or MM:SS
     */
    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return if (hours > 0) {
            String.format("%d:%02d", hours, minutes)
        } else {
            String.format("%d мин", minutes)
        }
    }

    /**
     * Format seconds as HH:MM or MM:SS
     */
    private fun formatSeconds(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) {
            String.format("%02d:%02d", hours, minutes)
        } else {
            String.format("%02d:%02d", 0, minutes)
        }
    }

    /**
     * Get or create notification settings for a chat
     */
    fun getNotificationSettings(chatId: Long): TwitchNotificationSettings {
        return settingsRepository.findByChatId(chatId)
            ?: TwitchNotificationSettings.createNew(chatId)
    }

    /**
     * Update notification settings
     */
    fun updateNotificationSettings(
        chatId: Long,
        messageTemplate: String,
        endedMessageTemplate: String,
        buttonText: String
    ): TwitchNotificationSettings {
        val existing = settingsRepository.findByChatId(chatId)

        val updated = if (existing != null) {
            existing.copy(
                messageTemplate = messageTemplate,
                endedMessageTemplate = endedMessageTemplate,
                buttonText = buttonText,
                updatedAt = Instant.now()
            )
        } else {
            // Use createNew with all parameters to preserve _isNew flag
            TwitchNotificationSettings.createNew(
                chatId = chatId,
                messageTemplate = messageTemplate,
                endedMessageTemplate = endedMessageTemplate,
                buttonText = buttonText
            )
        }

        return settingsRepository.save(updated)
    }
}
