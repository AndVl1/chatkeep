package ru.andvl.chatkeep.bot.handlers.channelreply

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.ChannelChat
import dev.inmo.tgbotapi.types.message.abstracts.FromChannelGroupContentMessage
// import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.service.channelreply.ChannelReplyService
// import ru.andvl.chatkeep.domain.service.channelreply.MediaGroupCacheService

@Component
class ChannelPostHandler(
    private val channelReplyService: ChannelReplyService
    // TODO: Add mediaGroupCacheService when media group handling is implemented
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

                // TODO: Add media group handling when ktgbotapi API is clearer
                // For now, reply to all messages including media groups

                // Get settings for chat
                val settings = withContext(Dispatchers.IO) {
                    channelReplyService.getSettings(chatId)
                }

                if (settings == null || !settings.enabled) {
                    logger.debug("Channel reply disabled or not configured for chat $chatId")
                    return@onContentMessage
                }

                if (settings.replyText.isNullOrBlank()) {
                    logger.debug("Channel reply text not set for chat $chatId")
                    return@onContentMessage
                }

                // Build URL keyboard if buttons configured
                val keyboard = if (!settings.buttonsJson.isNullOrBlank()) {
                    val buttons = channelReplyService.parseButtons(settings.buttonsJson)
                    if (buttons.isNotEmpty()) {
                        InlineKeyboardMarkup(
                            keyboard = matrix {
                                buttons.forEach { button ->
                                    row { +URLInlineKeyboardButton(button.text, button.url) }
                                }
                            }
                        )
                    } else null
                } else null

                // Send text-only reply (media support can be added later)
                reply(
                    to = message,
                    text = settings.replyText,
                    replyMarkup = keyboard
                )

                logger.info("Sent channel reply for chat $chatId")
            } catch (e: Exception) {
                logger.error("Failed to send channel reply: ${e.message}", e)
            }
        }
    }
}
