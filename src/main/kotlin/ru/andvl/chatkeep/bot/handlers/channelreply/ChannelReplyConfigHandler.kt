package ru.andvl.chatkeep.bot.handlers.channelreply

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.bot.util.SessionAuthHelper
import ru.andvl.chatkeep.domain.model.channelreply.ReplyButton
import ru.andvl.chatkeep.domain.service.channelreply.ChannelReplyService

@Component
class ChannelReplyConfigHandler(
    private val channelReplyService: ChannelReplyService,
    private val sessionAuthHelper: SessionAuthHelper
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_TEXT_LENGTH = 4096
        private const val MAX_BUTTON_TEXT_LENGTH = 64
        private const val MAX_BUTTONS = 10
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.scheme?.lowercase() in listOf("http", "https", "tg")
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun BehaviourContext.register() {
        val privateFilter = { msg: CommonMessage<*> -> msg.chat is PrivateChat }

        onCommand("setreply", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            handleSetReply(message)
        }

        onCommand("delreply", initialFilter = privateFilter) { message ->
            handleDelReply(message)
        }

        onCommand("showreply", initialFilter = privateFilter) { message ->
            handleShowReply(message)
        }

        onCommand("replybutton", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            handleReplyButton(message)
        }

        onCommand("clearbuttons", initialFilter = privateFilter) { message ->
            handleClearButtons(message)
        }

        // TODO: Media support - requires ktgbotapi API updates
        // onCommand("replymedia", initialFilter = privateFilter) { message ->
        //     handleReplyMedia(message)
        // }

        // onCommand("clearmedia", initialFilter = privateFilter) { message ->
        //     handleClearMedia(message)
        // }

        onCommand("replyenable", initialFilter = privateFilter) { message ->
            handleReplyEnable(message)
        }

        onCommand("replydisable", initialFilter = privateFilter) { message ->
            handleReplyDisable(message)
        }
    }

    private suspend fun BehaviourContext.handleSetReply(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val textContent = message.content as? TextContent
            val args = textContent?.text?.split(" ", limit = 2)?.drop(1) ?: emptyList()

            if (args.isEmpty()) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Usage: /setreply <text>")
                })
                return@withSessionAuth
            }

            val replyText = args[0]

            if (replyText.length > MAX_TEXT_LENGTH) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Текст слишком длинный. Максимум $MAX_TEXT_LENGTH символов.")
                })
                return@withSessionAuth
            }

            withContext(Dispatchers.IO) {
                channelReplyService.setText(ctx.chatId, replyText)
            }

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Channel reply text set.")
            })

            logger.info("Set channel reply text for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }

    private suspend fun BehaviourContext.handleDelReply(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            withContext(Dispatchers.IO) {
                channelReplyService.deleteSettings(ctx.chatId)
            }

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Channel reply settings deleted.")
            })

            logger.info("Deleted channel reply settings for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }

    private suspend fun BehaviourContext.handleShowReply(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val settings = withContext(Dispatchers.IO) {
                channelReplyService.getSettings(ctx.chatId)
            }

            if (settings == null) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("No channel reply configured.")
                })
                return@withSessionAuth
            }

            val buttons = channelReplyService.parseButtons(settings.buttonsJson)

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                appendLine("Channel reply settings:")
                appendLine()
                appendLine("Enabled: ${if (settings.enabled) "YES" else "NO"}")
                appendLine("Text: ${settings.replyText ?: "Not set"}")
                appendLine("Media: ${settings.mediaType ?: "None"}")
                if (buttons.isNotEmpty()) {
                    appendLine("Buttons:")
                    buttons.forEach { button ->
                        appendLine("  • ${button.text} - ${button.url}")
                    }
                } else {
                    appendLine("Buttons: None")
                }
            })
        }
    }

    private suspend fun BehaviourContext.handleReplyButton(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val textContent = message.content as? TextContent
            val args = textContent?.text?.split(" ", limit = 3)?.drop(1) ?: emptyList()

            if (args.size < 2) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Usage: /replybutton <text> <url>")
                })
                return@withSessionAuth
            }

            val buttonText = args[0]
            val buttonUrl = args[1]

            // Validate button text length
            if (buttonText.length > MAX_BUTTON_TEXT_LENGTH) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Текст кнопки слишком длинный. Максимум $MAX_BUTTON_TEXT_LENGTH символов.")
                })
                return@withSessionAuth
            }

            // Validate URL
            if (!isValidUrl(buttonUrl)) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Неверный URL. Разрешены только http://, https://, tg:// ссылки.")
                })
                return@withSessionAuth
            }

            // Get existing buttons
            val settings = withContext(Dispatchers.IO) {
                channelReplyService.getSettings(ctx.chatId)
            }
            val existingButtons = channelReplyService.parseButtons(settings?.buttonsJson).toMutableList()

            // Check button count limit
            if (existingButtons.size >= MAX_BUTTONS) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Максимум $MAX_BUTTONS кнопок. Используйте /clearbuttons для очистки.")
                })
                return@withSessionAuth
            }

            // Add new button
            existingButtons.add(ReplyButton(buttonText, buttonUrl))

            withContext(Dispatchers.IO) {
                channelReplyService.setButtons(ctx.chatId, existingButtons)
            }

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Button added. Total buttons: ${existingButtons.size}")
            })

            logger.info("Added channel reply button for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }

    private suspend fun BehaviourContext.handleClearButtons(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            withContext(Dispatchers.IO) {
                channelReplyService.clearButtons(ctx.chatId)
            }

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("All buttons cleared.")
            })

            logger.info("Cleared channel reply buttons for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }

    // TODO: Implement when ktgbotapi reply API is clearer
    /*
    private suspend fun BehaviourContext.handleReplyMedia(message: CommonMessage<*>) {
        // Implementation commented out - needs ktgbotapi v22 reply handling
    }

    private suspend fun BehaviourContext.handleClearMedia(message: CommonMessage<*>) {
        // Implementation commented out - needs ktgbotapi v22 reply handling
    }
    */

    private suspend fun BehaviourContext.handleReplyEnable(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            withContext(Dispatchers.IO) {
                channelReplyService.setEnabled(ctx.chatId, true)
            }

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Channel auto-reply enabled.")
            })

            logger.info("Enabled channel reply for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }

    private suspend fun BehaviourContext.handleReplyDisable(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            withContext(Dispatchers.IO) {
                channelReplyService.setEnabled(ctx.chatId, false)
            }

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Channel auto-reply disabled.")
            })

            logger.info("Disabled channel reply for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }
}
