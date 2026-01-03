package ru.andvl.chatkeep.bot.util

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.moderation.AdminSession
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.AdminSessionService

/**
 * Context for private chat commands requiring session authentication.
 *
 * @property userId The user ID of the admin
 * @property chatId The connected chat ID from the session
 * @property session The admin session
 */
data class SessionContext(
    val userId: Long,
    val chatId: Long,
    val session: AdminSession
)

/**
 * Utility for session-based authentication in private chat commands.
 *
 * Validates that:
 * 1. User has an active session (connected to a chat)
 * 2. User is an admin in the connected chat
 */
@Component
class SessionAuthHelper(
    private val adminSessionService: AdminSessionService,
    private val adminCacheService: AdminCacheService
) {
    /**
     * Execute a block with session authentication.
     *
     * @param context The BehaviourContext for bot operations
     * @param message The incoming message
     * @param block The block to execute if authentication succeeds
     * @return The result of the block, or null if authentication failed
     */
    suspend fun <T> withSessionAuth(
        context: BehaviourContext,
        message: CommonMessage<*>,
        block: suspend BehaviourContext.(SessionContext) -> T
    ): T? {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return null

        val session = withContext(Dispatchers.IO) {
            adminSessionService.getSession(userId)
        } ?: run {
            context.reply(message, "You must be connected to a chat first. Use /connect <chat_id>")
            return null
        }

        val chatId = session.connectedChatId

        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(userId, chatId)
        }

        if (!isAdmin) {
            context.reply(message, "You are not an admin in the connected chat.")
            return null
        }

        return context.block(SessionContext(userId, chatId, session))
    }

    /**
     * Format a reply prefix with the connected chat info.
     */
    fun formatReplyPrefix(session: AdminSession): String =
        adminSessionService.formatReplyPrefix(session)
}
