package ru.andvl.chatkeep.bot.util

import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.MessageDataCallbackQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

/**
 * Parsed callback data with admin verification.
 *
 * @property chatId The chat ID from callback data
 * @property targetUserId The target user ID from callback data
 * @property clickerId The user ID who clicked the button
 * @property extra Optional extra data (e.g., action type)
 */
data class ParsedCallback(
    val chatId: Long,
    val targetUserId: Long,
    val clickerId: Long,
    val extra: String? = null
)

/**
 * Utility for parsing callback data and verifying admin permissions.
 */
@Component
class CallbackParseHelper(
    private val adminCacheService: AdminCacheService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Parse callback data and verify admin permissions.
     *
     * Expected format: prefix:chatId:userId[:extra]
     *
     * @param context The BehaviourContext for bot operations
     * @param query The callback query
     * @param expectedParts Expected number of parts (including prefix)
     * @param forceRefresh Whether to force refresh admin status (recommended for security)
     * @return Parsed callback data, or null if parsing/validation failed
     */
    suspend fun parseAndVerifyAdmin(
        context: BehaviourContext,
        query: MessageDataCallbackQuery,
        expectedParts: Int,
        forceRefresh: Boolean = true
    ): ParsedCallback? {
        val data = query.data
        val parts = data.split(":")

        if (parts.size != expectedParts) {
            context.answer(query, "Неверный формат данных", showAlert = true)
            logger.warn("Invalid callback data format: $data (expected $expectedParts parts, got ${parts.size})")
            return null
        }

        val chatId = parts[1].toLongOrNull()
        val targetUserId = parts[2].toLongOrNull()
        val extra = if (parts.size > 3) parts[3] else null

        if (chatId == null || targetUserId == null) {
            context.answer(query, "Ошибка обработки данных", showAlert = true)
            logger.warn("Invalid chatId or targetUserId in callback: $data")
            return null
        }

        val clickerId = query.user.id.chatId.long

        // Check if clicker is admin (forceRefresh for security - prevents cached permission bypass)
        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(clickerId, chatId, forceRefresh = forceRefresh)
        }

        if (!isAdmin) {
            context.answer(query, "Только администратор может выполнить это действие", showAlert = true)
            logger.debug("Non-admin user $clickerId attempted callback action in chat $chatId")
            return null
        }

        return ParsedCallback(chatId, targetUserId, clickerId, extra)
    }
}
