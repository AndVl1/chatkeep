package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.bot.util.CallbackParseHelper
import ru.andvl.chatkeep.domain.service.moderation.WarningService

@Component
class WarningCallbackHandler(
    private val callbackParseHelper: CallbackParseHelper,
    private val warningService: WarningService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        onMessageDataCallbackQuery(Regex("warn_del:.*")) { query ->
            // Parse and verify admin (3 parts: prefix:chatId:userId)
            val parsed = callbackParseHelper.parseAndVerifyAdmin(this, query, expectedParts = 3)
                ?: return@onMessageDataCallbackQuery

            // Remove warnings from database
            try {
                withContext(Dispatchers.IO) {
                    warningService.removeWarnings(parsed.chatId, parsed.targetUserId, parsed.clickerId)
                }
            } catch (e: Exception) {
                answer(query, "Ошибка удаления варна из базы", showAlert = true)
                logger.error("Failed to remove warnings for user ${parsed.targetUserId} in chat ${parsed.chatId}: ${e.message}", e)
                return@onMessageDataCallbackQuery
            }

            // Delete the warning notification message
            try {
                delete(query.message)
                answer(query, "Варн удален")
                logger.info("Warning deleted by admin ${parsed.clickerId} in chat ${parsed.chatId} for user ${parsed.targetUserId}")
            } catch (e: Exception) {
                // Warning was removed from DB, but message deletion failed - still success
                answer(query, "Варн удален")
                logger.warn("Warning removed but message deletion failed in chat ${parsed.chatId}: ${e.message}")
            }
        }
    }
}
