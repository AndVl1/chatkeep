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
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.moderation.PunishmentService
import ru.andvl.chatkeep.domain.service.moderation.WarningService

@Component
class BlocklistCallbackHandler(
    private val callbackParseHelper: CallbackParseHelper,
    private val warningService: WarningService,
    private val punishmentService: PunishmentService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        // Callback format: block_undo:chatId:userId:action
        onMessageDataCallbackQuery(Regex("block_undo:.*")) { query ->
            // Parse and verify admin (4 parts: prefix:chatId:userId:action)
            val parsed = callbackParseHelper.parseAndVerifyAdmin(this, query, expectedParts = 4)
                ?: return@onMessageDataCallbackQuery

            val actionStr = parsed.extra ?: run {
                answer(query, "Неизвестное действие", showAlert = true)
                return@onMessageDataCallbackQuery
            }

            val action = try {
                PunishmentType.valueOf(actionStr)
            } catch (e: IllegalArgumentException) {
                answer(query, "Неизвестное действие", showAlert = true)
                logger.warn("Invalid action in blocklist callback: $actionStr")
                return@onMessageDataCallbackQuery
            }

            // Undo the punishment based on action type
            try {
                val success = when (action) {
                    PunishmentType.WARN -> {
                        withContext(Dispatchers.IO) {
                            warningService.removeWarnings(parsed.chatId, parsed.targetUserId, parsed.clickerId)
                        }
                        true
                    }
                    PunishmentType.MUTE -> {
                        withContext(Dispatchers.IO) {
                            punishmentService.unmute(parsed.chatId, parsed.targetUserId, parsed.clickerId)
                        }
                    }
                    PunishmentType.BAN -> {
                        withContext(Dispatchers.IO) {
                            punishmentService.unban(parsed.chatId, parsed.targetUserId, parsed.clickerId)
                        }
                    }
                    PunishmentType.KICK -> {
                        // KICK = ban + immediate unban, user is already unbanned
                        // Nothing to undo, just acknowledge
                        true
                    }
                    PunishmentType.NOTHING -> {
                        // Should not reach here, but handle gracefully
                        true
                    }
                }

                if (!success) {
                    answer(query, "Не удалось отменить действие", showAlert = true)
                    return@onMessageDataCallbackQuery
                }

                // Delete the notification message
                try {
                    delete(query.message)
                } catch (e: Exception) {
                    logger.warn("Failed to delete blocklist notification in chat ${parsed.chatId}: ${e.message}")
                }

                val actionName = when (action) {
                    PunishmentType.WARN -> "Варн удален"
                    PunishmentType.MUTE -> "Мут снят"
                    PunishmentType.BAN -> "Бан снят"
                    PunishmentType.KICK -> "Уведомление закрыто"
                    PunishmentType.NOTHING -> "Действие отменено"
                }
                answer(query, actionName)
                logger.info("Blocklist action $action undone by admin ${parsed.clickerId} in chat ${parsed.chatId} for user ${parsed.targetUserId}")

            } catch (e: Exception) {
                answer(query, "Ошибка отмены действия", showAlert = true)
                logger.error("Failed to undo blocklist action $action for user ${parsed.targetUserId} in chat ${parsed.chatId}", e)
            }
        }
    }
}
