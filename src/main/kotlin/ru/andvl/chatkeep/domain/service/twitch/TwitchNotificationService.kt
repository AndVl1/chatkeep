package ru.andvl.chatkeep.domain.service.twitch

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.requests.chat.modify.PinChatMessage
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
    private val timelineRepository: StreamTimelineEventRepository,
    private val telegraphService: TelegraphService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Send stream start notification
     * Returns pair of (messageId, hasPhoto)
     */
    suspend fun sendStreamStartNotification(
        chatId: Long,
        stream: TwitchStream,
        streamerName: String,
        streamerLogin: String,
        thumbnailUrl: String?,
        isPinned: Boolean = false,
        pinSilently: Boolean = true
    ): Pair<Long, Boolean>? {
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

            val hasPhoto = thumbnail != null
            val message = if (hasPhoto) {
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

            // Pin message if needed
            if (isPinned) {
                try {
                    bot.execute(
                        PinChatMessage(
                            chatId = chatId.toChatId(),
                            messageId = message.messageId,
                            disableNotification = pinSilently
                        )
                    )
                    logger.info("Pinned stream notification: chatId=$chatId, messageId=${message.messageId}, silently=$pinSilently")
                } catch (e: Exception) {
                    logger.error("Failed to pin stream notification: chatId=$chatId, messageId=${message.messageId}", e)
                }
            }

            logger.info("Sent stream start notification: chatId=$chatId, messageId=${message.messageId}, stream=${stream.id}, hasPhoto=$hasPhoto")
            return Pair(message.messageId.long, hasPhoto)
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
        timeline: List<StreamTimelineEvent>,
        thumbnailUrl: String? = null
    ): String? {
        try {
            val settings = settingsRepository.findByChatId(chatId)
                ?: TwitchNotificationSettings.createNew(chatId)

            // Format caption first WITHOUT Telegraph URL to check length
            val captionWithoutTelegraph = formatStreamCaption(
                settings = settings,
                stream = stream,
                streamerName = streamerName,
                timeline = timeline,
                telegraphUrl = null
            )

            // Create Telegraph page ONLY if caption > 1000 characters
            val telegraphUrl = if (captionWithoutTelegraph.length > 1000) {
                logger.info("Caption length ${captionWithoutTelegraph.length} > 1000, creating Telegraph page for stream ${stream.id}")
                telegraphService.createOrUpdateTimelinePage(
                    streamerId = streamerLogin,
                    streamerName = streamerName,
                    timeline = timeline
                )
            } else {
                logger.info("Caption length ${captionWithoutTelegraph.length} <= 1000, skipping Telegraph page for stream ${stream.id}")
                null
            }

            // Use caption without Telegraph URL (we don't add link to caption, only button)
            val caption = captionWithoutTelegraph

            // Check if we need to add preview button (thumbnail fallback)
            val previewUrl = if (!stream.hasPhoto && thumbnailUrl != null) {
                thumbnailUrl.replace("{width}", "1280").replace("{height}", "720")
            } else {
                null
            }

            // Build keyboard with stream link button and optional Telegraph/preview buttons
            val keyboard = buildKeyboard(
                streamLink = "https://twitch.tv/$streamerLogin",
                buttonText = settings.buttonText,
                telegraphUrl = telegraphUrl,
                previewUrl = previewUrl
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

            logger.info("Updated stream notification: chatId=$chatId, messageId=$messageId, telegraphUrl=$telegraphUrl")
            return telegraphUrl
        } catch (e: Exception) {
            logger.error("Failed to update stream notification: chatId=$chatId, messageId=$messageId", e)
            return null
        }
    }

    /**
     * Format stream caption using template with smart timeline compression
     */
    private fun formatStreamCaption(
        settings: TwitchNotificationSettings,
        stream: TwitchStream,
        streamerName: String,
        timeline: List<StreamTimelineEvent>,
        telegraphUrl: String? = null
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

        logger.info("formatStreamCaption: isEnded=$isEnded, timelineSize=${timeline.size}, telegraphUrl=$telegraphUrl")

        var caption = template
            .replace("{streamer}", streamerName)
            .replace("{title}", stream.currentTitle ?: "")
            .replace("{game}", stream.currentGame ?: "Just Chatting")
            .replace("{viewers}", stream.viewerCount.toString())
            .replace("{duration}", duration)

        // Add timeline: try full timeline first, compress only if exceeds limit
        if (timeline.isNotEmpty() && (isEnded || timeline.size > 1)) {
            // Format full timeline
            val fullTimeline = formatFullTimeline(timeline)
            val captionWithFullTimeline = caption + "\n\n📋 Таймлайн:\n$fullTimeline"

            // Check if full timeline fits within limit
            if (captionWithFullTimeline.length <= 1000) {
                caption = captionWithFullTimeline
            } else {
                // Timeline too long, use compressed version
                val compressedTimeline = compressTimeline(timeline, maxLength = 800)
                if (compressedTimeline.isNotEmpty()) {
                    caption += "\n\n📋 Таймлайн:\n$compressedTimeline"
                }
            }
            // Note: Telegraph link is NOT added to caption, only as button
        }

        return caption
    }

    /**
     * Format full timeline (all events)
     */
    private fun formatFullTimeline(timeline: List<StreamTimelineEvent>): String {
        if (timeline.isEmpty()) return ""

        return timeline.joinToString("\n") { event ->
            val time = formatSeconds(event.streamOffsetSeconds)
            val game = event.gameName ?: "Just Chatting"
            val title = event.streamTitle?.take(50) ?: ""
            val titlePart = if (title.isNotEmpty()) " | $title" else ""
            "$time - $game$titlePart"
        }
    }

    /**
     * Compress timeline to fit within character limit
     * Intelligently selects events to show: game changes + evenly distributed samples
     */
    private fun compressTimeline(timeline: List<StreamTimelineEvent>, maxLength: Int): String {
        if (timeline.isEmpty()) return ""
        if (timeline.size == 1) return formatFullTimeline(timeline)

        val first = timeline.first()
        val last = timeline.last()

        // Find all game change events
        val gameChanges = timeline.filterIndexed { index, event ->
            val prevEvent = timeline.getOrNull(index - 1)
            prevEvent == null || prevEvent.gameName != event.gameName
        }

        // Build initial entry list: first + game changes + last
        val entries = mutableListOf<StreamTimelineEvent>()
        entries.add(first)
        gameChanges.forEach { event ->
            if (event != first && event != last) {
                entries.add(event)
            }
        }
        if (last != first) {
            entries.add(last)
        }

        // If very few entries (no game changes), add evenly distributed samples
        if (entries.size <= 3 && timeline.size > 3) {
            val step = timeline.size / 5  // Sample ~5 events evenly
            for (i in step until timeline.size - 1 step step) {
                val event = timeline[i]
                if (event != first && event != last && !entries.contains(event)) {
                    entries.add(event)
                }
            }
            entries.sortBy { it.streamOffsetSeconds }
        }

        // Format entries
        val formattedEntries = entries.map { event ->
            val time = formatSeconds(event.streamOffsetSeconds)
            val game = event.gameName ?: "Just Chatting"
            val title = event.streamTitle?.take(50) ?: ""
            val titlePart = if (title.isNotEmpty()) " | $title" else ""
            "$time - $game$titlePart"
        }

        // Try to fit as many entries as possible within maxLength
        var result = formattedEntries.joinToString("\n")

        // If still too long, progressively remove middle entries
        if (result.length > maxLength && entries.size > 3) {
            var kept = entries.toMutableList()
            while (kept.size > 2 && result.length > maxLength) {
                // Remove middle entry
                val midIndex = kept.size / 2
                kept.removeAt(midIndex)

                result = kept.joinToString("\n") { event ->
                    val time = formatSeconds(event.streamOffsetSeconds)
                    val game = event.gameName ?: "Just Chatting"
                    val title = event.streamTitle?.take(50) ?: ""
                    val titlePart = if (title.isNotEmpty()) " | $title" else ""
                    "$time - $game$titlePart"
                }
            }

            val removed = timeline.size - kept.size
            if (removed > 0) {
                result += "\n... (+$removed записей в полном таймлайне)"
            }
        }

        return result
    }

    /**
     * Build inline keyboard with stream link and optional Telegraph/preview buttons
     */
    private fun buildKeyboard(
        streamLink: String,
        buttonText: String,
        telegraphUrl: String?,
        previewUrl: String? = null
    ): InlineKeyboardMarkup {
        val rows = mutableListOf<List<URLInlineKeyboardButton>>()

        // Stream link button
        rows.add(listOf(URLInlineKeyboardButton(buttonText, streamLink)))

        // Telegraph button (if URL provided)
        if (telegraphUrl != null) {
            rows.add(listOf(URLInlineKeyboardButton("📋 Полный таймлайн", telegraphUrl)))
        }

        // Preview button (if thumbnail became available after initial send)
        if (previewUrl != null) {
            rows.add(listOf(URLInlineKeyboardButton("📸 Превью", previewUrl)))
        }

        return InlineKeyboardMarkup(keyboard = rows)
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
