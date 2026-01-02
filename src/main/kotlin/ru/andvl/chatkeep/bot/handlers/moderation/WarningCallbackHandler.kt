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
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.WarningService

@Component
class WarningCallbackHandler(
    private val adminCacheService: AdminCacheService,
    private val warningService: WarningService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        onMessageDataCallbackQuery(Regex("warn_del:.*")) { query ->
            val data = query.data
            val parts = data.split(":")

            if (parts.size != 3) {
                answer(query, "Неверный формат данных", showAlert = true)
                logger.warn("Invalid callback data format: $data")
                return@onMessageDataCallbackQuery
            }

            val chatId = parts[1].toLongOrNull()
            val targetUserId = parts[2].toLongOrNull()

            if (chatId == null || targetUserId == null) {
                answer(query, "Ошибка обработки данных", showAlert = true)
                logger.warn("Invalid chatId or targetUserId in callback: $data")
                return@onMessageDataCallbackQuery
            }

            val clickerId = query.user.id.chatId.long

            // Check if clicker is admin
            val isAdmin = withContext(Dispatchers.IO) {
                adminCacheService.isAdmin(clickerId, chatId)
            }

            if (!isAdmin) {
                answer(query, "Только администратор может удалить это сообщение", showAlert = true)
                logger.debug("Non-admin user $clickerId attempted to delete warning in chat $chatId")
                return@onMessageDataCallbackQuery
            }

            // Remove warnings from database
            withContext(Dispatchers.IO) {
                warningService.removeWarnings(chatId, targetUserId, clickerId)
            }

            // Delete the warning notification message
            try {
                delete(query.message)
                answer(query, "Варн удален")
                logger.info("Warning deleted by admin $clickerId in chat $chatId for user $targetUserId")
            } catch (e: Exception) {
                answer(query, "Не удалось удалить сообщение", showAlert = true)
                logger.warn("Failed to delete warning message in chat $chatId: ${e.message}")
            }
        }
    }
}
