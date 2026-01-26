package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.service.AdminErrorNotificationService
import ru.andvl.chatkeep.bot.service.ErrorContext
import ru.andvl.chatkeep.domain.service.RulesService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@Component
class RulesCommandHandler(
    private val rulesService: RulesService,
    private val adminCacheService: AdminCacheService,
    private val errorNotificationService: AdminErrorNotificationService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        // /rules - Show chat rules
        onCommand("rules") { message ->
            if (message.chat !is GroupChat && message.chat !is SupergroupChat) {
                reply(message, "This command can only be used in group chats")
                return@onCommand
            }

            val chatId = message.chat.id.chatId.long

            withContext(Dispatchers.IO) {
                try {
                    val rules = rulesService.getRules(chatId)
                    if (rules == null) {
                        reply(message, "No rules have been set for this chat")
                    } else {
                        reply(message, "📜 Chat Rules:\n\n${rules.rulesText}")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to get rules: ${e.message}", e)
                    errorNotificationService.reportHandlerError(
                        handler = "RulesCommandHandler",
                        error = e,
                        context = ErrorContext(chatId = chatId, command = "/rules")
                    )
                    reply(message, "❌ Failed to retrieve rules")
                }
            }
        }

        // /setrules - Set chat rules (admin only)
        onCommand("setrules", requireOnlyCommandInMessage = false) { message ->
            if (message.chat !is GroupChat && message.chat !is SupergroupChat) {
                reply(message, "This command can only be used in group chats")
                return@onCommand
            }

            val userId = (message as? dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage)?.from?.id?.chatId?.long ?: return@onCommand
            val chatId = message.chat.id.chatId.long

            // Check if user is admin
            val isAdmin = withContext(Dispatchers.IO) {
                adminCacheService.isAdmin(userId, chatId, forceRefresh = false)
            }

            if (!isAdmin) {
                reply(message, "❌ Only admins can set chat rules")
                return@onCommand
            }

            // Extract rules text from command
            val messageText = (message.content as? dev.inmo.tgbotapi.types.message.content.TextContent)?.text
                ?: return@onCommand

            val rulesText = messageText.substringAfter("/setrules").trim()
            if (rulesText.isEmpty()) {
                reply(message, "Usage: /setrules [rules text]")
                return@onCommand
            }

            withContext(Dispatchers.IO) {
                try {
                    rulesService.setRules(chatId, rulesText)
                    reply(message, "✅ Chat rules have been updated")
                    logger.info("Rules set for chatId=$chatId by userId=$userId")
                } catch (e: Exception) {
                    logger.error("Failed to set rules: ${e.message}", e)
                    errorNotificationService.reportHandlerError(
                        handler = "RulesCommandHandler",
                        error = e,
                        context = ErrorContext(chatId = chatId, userId = userId, command = "/setrules")
                    )
                    reply(message, "❌ Failed to set rules")
                }
            }
        }
    }
}
