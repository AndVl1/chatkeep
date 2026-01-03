package ru.andvl.chatkeep.bot.handlers.channelreply

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.replyWithAnimation
import dev.inmo.tgbotapi.extensions.api.send.replyWithDocument
import dev.inmo.tgbotapi.extensions.api.send.replyWithPhoto
import dev.inmo.tgbotapi.extensions.api.send.replyWithVideo
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.ChannelChat
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromChannelGroupContentMessage
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.model.channelreply.ChannelReplySettings
import ru.andvl.chatkeep.domain.model.channelreply.MediaType
import ru.andvl.chatkeep.domain.service.channelreply.ChannelReplyService

@Component
class ChannelPostHandler(
    private val channelReplyService: ChannelReplyService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @OptIn(RiskFeature::class)
    override suspend fun BehaviourContext.register() {
        onContentMessage(
            initialFilter = { message ->
                // Filter: messages from channels in discussion chats
                message is FromChannelGroupContentMessage<*> && message.chat !is ChannelChat
            },
            markerFactory = null
        ) { message ->
            try {
                val chatId = message.chat.id.chatId.long

                // Get settings for chat
                val settings = withContext(Dispatchers.IO) {
                    channelReplyService.getSettings(chatId)
                }

                if (settings == null || !settings.enabled) {
                    logger.debug("Channel reply disabled or not configured for chat $chatId")
                    return@onContentMessage
                }

                // Check if we have something to send (text or media)
                val hasText = !settings.replyText.isNullOrBlank()
                val hasMedia = !settings.mediaFileId.isNullOrBlank() && !settings.mediaType.isNullOrBlank()

                if (!hasText && !hasMedia) {
                    logger.debug("Channel reply has no text or media for chat $chatId")
                    return@onContentMessage
                }

                // Build URL keyboard if buttons configured
                val keyboard = buildKeyboard(settings)

                // Send reply with media or text-only
                if (hasMedia) {
                    sendMediaReply(message, settings, keyboard)
                } else {
                    reply(
                        to = message,
                        text = settings.replyText!!,
                        replyMarkup = keyboard
                    )
                }

                logger.info("Sent channel reply for chat $chatId")
            } catch (e: Exception) {
                logger.error("Failed to send channel reply: ${e.message}", e)
            }
        }
    }

    private fun buildKeyboard(settings: ChannelReplySettings): InlineKeyboardMarkup? {
        if (settings.buttonsJson.isNullOrBlank()) return null
        val buttons = channelReplyService.parseButtons(settings.buttonsJson)
        if (buttons.isEmpty()) return null
        return InlineKeyboardMarkup(
            keyboard = matrix {
                buttons.forEach { button ->
                    row { +URLInlineKeyboardButton(button.text, button.url) }
                }
            }
        )
    }

    private suspend fun BehaviourContext.sendMediaReply(
        message: AccessibleMessage,
        settings: ChannelReplySettings,
        keyboard: InlineKeyboardMarkup?
    ) {
        val fileId = FileId(settings.mediaFileId!!)
        val caption = settings.replyText // Can be null

        when (MediaType.valueOf(settings.mediaType!!)) {
            MediaType.PHOTO -> replyWithPhoto(
                to = message,
                fileId = fileId,
                text = caption,
                replyMarkup = keyboard
            )
            MediaType.VIDEO -> replyWithVideo(
                to = message,
                video = fileId,
                text = caption,
                replyMarkup = keyboard
            )
            MediaType.DOCUMENT -> replyWithDocument(
                to = message,
                document = fileId,
                text = caption,
                replyMarkup = keyboard
            )
            MediaType.ANIMATION -> replyWithAnimation(
                to = message,
                animation = fileId,
                text = caption,
                replyMarkup = keyboard
            )
        }
    }
}
