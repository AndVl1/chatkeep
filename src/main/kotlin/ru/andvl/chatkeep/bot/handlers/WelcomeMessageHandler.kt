package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onNewChatMembers
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.types.UserId
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.service.AdminErrorNotificationService
import ru.andvl.chatkeep.bot.service.ErrorContext
import ru.andvl.chatkeep.domain.service.WelcomeService

@Component
class WelcomeMessageHandler(
    private val welcomeService: WelcomeService,
    private val errorNotificationService: AdminErrorNotificationService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        onNewChatMembers { message ->
            val chatId = message.chat.id.chatId.long

            withContext(Dispatchers.IO) {
                try {
                    val settings = welcomeService.getWelcomeSettings(chatId)
                    if (settings == null || !settings.enabled) {
                        return@withContext
                    }

                    // Get welcome message text
                    val welcomeText = settings.messageText
                        ?: "Welcome to the chat!"

                    // Send welcome message
                    if (settings.sendToChat) {
                        message.chatEvent.members.forEach { user ->
                            // Skip bots
                            if (user is dev.inmo.tgbotapi.types.chat.CommonBot) return@forEach

                            val personalizedMessage = welcomeText
                                .replace("{user}", user.firstName)
                                .replace("{username}", user.username?.username ?: user.firstName)

                            val sentMessage = sendTextMessage(
                                message.chat,
                                personalizedMessage
                            )

                            // Auto-delete if configured
                            settings.deleteAfterSeconds?.let { seconds ->
                                launch {
                                    delay(seconds * 1000L)
                                    try {
                                        deleteMessage(sentMessage)
                                    } catch (e: Exception) {
                                        logger.debug("Failed to delete welcome message: ${e.message}")
                                    }
                                }
                            }
                        }
                    }

                    logger.info("Sent welcome message for chatId=$chatId")
                } catch (e: Exception) {
                    logger.error("Failed to send welcome message: ${e.message}", e)
                    errorNotificationService.reportHandlerError(
                        handler = "WelcomeMessageHandler",
                        error = e,
                        context = ErrorContext(chatId = chatId)
                    )
                }
            }
        }
    }
}
