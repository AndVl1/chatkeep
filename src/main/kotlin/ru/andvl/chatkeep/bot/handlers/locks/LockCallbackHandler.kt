package ru.andvl.chatkeep.bot.handlers.locks

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

/**
 * Handles callback queries for lock violation notifications.
 * Allows admins to remove warnings issued by lock enforcement.
 */
@Component
class LockCallbackHandler(
    private val callbackParseHelper: CallbackParseHelper,
    private val warningService: WarningService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        // Callback format: lock_undo:chatId:userId:WARN
        onMessageDataCallbackQuery(Regex("lock_undo:.*")) { query ->
            // Parse and verify admin (4 parts: prefix:chatId:userId:action)
            val parsed = callbackParseHelper.parseAndVerifyAdmin(this, query, expectedParts = 4)
                ?: return@onMessageDataCallbackQuery

            val actionStr = parsed.extra
            if (actionStr != "WARN") {
                answer(query, "Неизвестное действие", showAlert = true)
                logger.warn("Invalid action in lock callback: $actionStr")
                return@onMessageDataCallbackQuery
            }

            try {
                // Remove the warning
                withContext(Dispatchers.IO) {
                    warningService.removeWarnings(parsed.chatId, parsed.targetUserId, parsed.clickerId)
                }

                // Delete the notification message
                try {
                    delete(query.message)
                } catch (e: Exception) {
                    logger.warn("Failed to delete lock notification in chat ${parsed.chatId}: ${e.message}")
                }

                answer(query, "Варн удален")
                logger.info("Lock warning removed by admin ${parsed.clickerId} in chat ${parsed.chatId} for user ${parsed.targetUserId}")

            } catch (e: Exception) {
                answer(query, "Ошибка удаления варна", showAlert = true)
                logger.error("Failed to remove lock warning for user ${parsed.targetUserId} in chat ${parsed.chatId}", e)
            }
        }
    }
}
