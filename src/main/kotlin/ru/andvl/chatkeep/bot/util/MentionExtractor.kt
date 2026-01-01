package ru.andvl.chatkeep.bot.util

import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.logger
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.TextMentionTextSource

/**
 * Result of extracting user ID and command arguments from a message.
 *
 * @property userId The extracted user ID, or null if not found
 * @property arguments The command arguments (excluding command and user identifier)
 * @property source How the user ID was identified
 * @property username Plain @username if found but not yet resolved to ID
 */
data class UserExtractionResult(
    val userId: Long?,
    val arguments: List<String>,
    val source: UserIdSource,
    val username: String? = null
)

enum class UserIdSource {
    REPLY,           // User ID from replied message
    TEXT_MENTION,    // User ID from clickable @mention entity
    NUMERIC_ARG,     // User ID from numeric argument in text
    USERNAME_LOOKUP, // User ID resolved from @username via API lookup
    NOT_FOUND        // User ID could not be extracted
}

object MentionExtractor {

    /**
     * Extracts target user ID and remaining arguments from a moderation command message.
     *
     * Supported formats (in priority order):
     * 1. Reply to user's message - extracts user from the replied message
     * 2. TextMention entity - clickable @mention with embedded user ID
     * 3. Numeric user ID - e.g., "/mute 123456789"
     *
     * @return UserExtractionResult containing user ID, remaining arguments, and source
     */
    fun extractUserAndArgs(message: CommonMessage<*>): UserExtractionResult {
        logger.info("abcqwe extracting user $message")
        val content = message.content as? TextContent
        val allArgs = content?.text?.let { extractArguments(it) } ?: emptyList()

        // Priority 1: Check if message is a reply
        val replyToMessage = message.replyTo as? FromUserMessage
        replyToMessage?.from?.id?.chatId?.long?.let { userId ->
            // Reply found - all args are command arguments (no user ID in text to skip)
            return UserExtractionResult(userId, allArgs, UserIdSource.REPLY)
        }

        if (content == null) {
            return UserExtractionResult(null, emptyList(), UserIdSource.NOT_FOUND)
        }

        // Priority 2: Check for TextMentionTextSource (clickable mention with user ID)
        val textMention = content.textSources.filterIsInstance<TextMentionTextSource>().firstOrNull()
        if (textMention != null) {
            logger.info("abcqwe $textMention")
            val userId = textMention.user.id.chatId.long
            // TextMention found - the mention text is in args[0], skip it
            val remainingArgs = if (allArgs.isNotEmpty()) allArgs.drop(1) else emptyList()
            return UserExtractionResult(userId, remainingArgs, UserIdSource.TEXT_MENTION)
        }

        // Priority 3: Try to extract numeric user ID from first argument
        val firstArg = allArgs.firstOrNull()
        val numericUserId = firstArg?.toLongOrNull()
        if (numericUserId != null) {
            // Numeric ID found in args[0] - skip it
            val remainingArgs = allArgs.drop(1)
            return UserExtractionResult(numericUserId, remainingArgs, UserIdSource.NUMERIC_ARG)
        }

        // Priority 4: Check if first argument is a plain @username (needs API lookup)
        if (firstArg != null && firstArg.startsWith("@") && firstArg.length > 1) {
            val username = firstArg.removePrefix("@")
            val remainingArgs = allArgs.drop(1)
            // Return with username for later resolution via API
            return UserExtractionResult(null, remainingArgs, UserIdSource.NOT_FOUND, username)
        }

        // No user ID found
        return UserExtractionResult(null, allArgs, UserIdSource.NOT_FOUND)
    }

    /**
     * Extracts target user ID from a moderation command message.
     * Use extractUserAndArgs() if you also need the remaining arguments.
     */
    fun extractUserId(message: CommonMessage<*>): Long? {
        return extractUserAndArgs(message).userId
    }

    /**
     * Extracts user ID specifically from reply message.
     */
    fun extractUserIdFromReply(message: CommonMessage<*>): Long? {
        val replyToMessage = message.replyTo as? FromUserMessage
        return replyToMessage?.from?.id?.chatId?.long
    }

    /**
     * Extracts command arguments from message text.
     * Handles commands with bot username like "/mute@botname arg1 arg2".
     *
     * @return list of arguments (excluding the command itself)
     */
    fun extractArguments(text: String): List<String> {
        val parts = text.trim().split(Regex("\\s+"))
        if (parts.isEmpty()) return emptyList()

        // First part is the command, possibly with @botname attached
        // e.g., "/mute" or "/mute@chatkeep_bot"
        // We skip it and return the rest
        return parts.drop(1)
    }
}
