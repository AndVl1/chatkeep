package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.model.moderation.MatchType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.AdminSessionService
import ru.andvl.chatkeep.domain.service.moderation.BlocklistService

@Component
class BlocklistManagementHandler(
    private val blocklistService: BlocklistService,
    private val adminSessionService: AdminSessionService,
    private val adminCacheService: AdminCacheService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        val privateFilter = { msg: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*> ->
            msg.chat is PrivateChat
        }

        onCommand("addblock", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            logger.info("Received /addblock command from chat ${message.chat.id}")
            handleAddBlock(message)
        }

        onCommand("delblock", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            logger.info("Received /delblock command from chat ${message.chat.id}")
            handleDelBlock(message)
        }

        onCommand("blocklist", initialFilter = privateFilter) { message ->
            logger.info("Received /blocklist command from chat ${message.chat.id}")
            handleBlocklist(message)
        }
    }

    private suspend fun BehaviourContext.handleAddBlock(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        try {
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: run {
                logger.warn("/addblock: Cannot extract user ID from message type ${message::class.simpleName}")
                return
            }
            logger.info("/addblock: userId=$userId")

            val session = withContext(Dispatchers.IO) {
                adminSessionService.getSession(userId)
            } ?: run {
                logger.info("/addblock: No session for userId=$userId, sending connect message")
                reply(message, "You must be connected to a chat first. Use /connect <chat_id>")
                logger.info("/addblock: connect message sent")
                return
            }
            logger.info("/addblock: session found for chatId=${session.connectedChatId}")

        val chatId = session.connectedChatId

        // Verify admin status
        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(userId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You are not an admin in the connected chat.")
            return
        }

        val textContent = message.content as? TextContent
        val fullText = textContent?.text?.substringAfter("/addblock")?.trim() ?: ""

        if (fullText.isEmpty()) {
            reply(
                message,
                """
                Usage: /addblock <pattern> {action} {duration}

                Examples:
                /addblock spam
                /addblock *spam* {warn}
                /addblock badword {mute 1h}
                /addblock scam {ban}

                Actions: nothing, warn, mute, ban, kick
                - nothing: just delete message, no punishment
                - If action not specified, uses chat's default action

                Duration (inside braces): 1h, 24h, 7d
                """.trimIndent()
            )
            return
        }

        // Parse pattern and optional {action duration} block
        // Format: pattern {action duration}
        val braceMatch = Regex("""\{([^}]+)\}""").find(fullText)
        val pattern: String
        val action: PunishmentType
        var duration: Int? = null

        if (braceMatch != null) {
            // Extract pattern (everything before the brace)
            pattern = fullText.substring(0, braceMatch.range.first).trim()

            // Parse content inside braces: "action" or "action duration"
            val braceContent = braceMatch.groupValues[1].trim().split(Regex("\\s+"))
            val actionStr = braceContent.firstOrNull()?.uppercase() ?: ""

            action = try {
                PunishmentType.valueOf(actionStr)
            } catch (e: Exception) {
                val sessionPrefix = adminSessionService.formatReplyPrefix(session)
                reply(message, "$sessionPrefix\n\nUnknown action: $actionStr\nValid actions: nothing, warn, mute, ban, kick")
                return
            }

            // Parse duration if present (second element in braces)
            if (braceContent.size > 1) {
                duration = ru.andvl.chatkeep.bot.util.DurationParser.parse(braceContent[1])?.let {
                    ru.andvl.chatkeep.bot.util.DurationParser.toHours(it)
                }
            }
        } else {
            // No braces - just pattern, use default action
            pattern = fullText.trim()
            action = withContext(Dispatchers.IO) {
                blocklistService.getDefaultAction(chatId)
            }
        }

        // Validate pattern
        if (pattern.isEmpty()) {
            val sessionPrefix = adminSessionService.formatReplyPrefix(session)
            reply(message, "$sessionPrefix\n\nPattern cannot be empty.")
            return
        }
        if (pattern.length > 100) {
            val sessionPrefix = adminSessionService.formatReplyPrefix(session)
            reply(message, "$sessionPrefix\n\nPattern too long. Maximum 100 characters.")
            return
        }

        val severity = 0

        val matchType = if (pattern.contains("*") || pattern.contains("?")) {
            MatchType.WILDCARD
        } else {
            MatchType.EXACT
        }

        withContext(Dispatchers.IO) {
            blocklistService.addPattern(chatId, pattern, matchType, action, duration, severity)
        }

        val prefix = adminSessionService.formatReplyPrefix(session)
        reply(message, "$prefix\n\nAdded blocklist pattern:\nPattern: $pattern\nAction: $action\nSeverity: $severity")
        logger.info("Blocklist pattern added: chatId=$chatId, pattern='$pattern', action=$action")
        } catch (e: Exception) {
            logger.error("/addblock: Exception occurred", e)
            try {
                reply(message, "Error: ${e.message}")
            } catch (replyError: Exception) {
                logger.error("/addblock: Failed to send error reply", replyError)
            }
        }
    }

    private suspend fun BehaviourContext.handleDelBlock(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: run {
            logger.warn("/delblock: Cannot extract user ID from message type ${message::class.simpleName}")
            return
        }

        val session = withContext(Dispatchers.IO) {
            adminSessionService.getSession(userId)
        } ?: run {
            reply(message, "You must be connected to a chat first. Use /connect <chat_id>")
            return
        }

        val chatId = session.connectedChatId

        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(userId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You are not an admin in the connected chat.")
            return
        }

        val textContent = message.content as? TextContent
        val args = textContent?.text?.split(" ")?.drop(1) ?: emptyList()

        if (args.isEmpty()) {
            reply(message, "Usage: /delblock <pattern>")
            return
        }

        val pattern = args[0]

        withContext(Dispatchers.IO) {
            blocklistService.removePattern(chatId, pattern)
        }

        val prefix = adminSessionService.formatReplyPrefix(session)
        reply(message, "$prefix\n\nRemoved blocklist pattern: $pattern")
        logger.info("Blocklist pattern removed: chatId=$chatId, pattern='$pattern'")
    }

    private suspend fun BehaviourContext.handleBlocklist(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: run {
            logger.warn("/blocklist: Cannot extract user ID from message type ${message::class.simpleName}")
            return
        }

        val session = withContext(Dispatchers.IO) {
            adminSessionService.getSession(userId)
        } ?: run {
            reply(message, "You must be connected to a chat first. Use /connect <chat_id>")
            return
        }

        val chatId = session.connectedChatId

        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(userId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You are not an admin in the connected chat.")
            return
        }

        val patterns = withContext(Dispatchers.IO) {
            blocklistService.listPatterns(chatId)
        }

        if (patterns.isEmpty()) {
            val prefix = adminSessionService.formatReplyPrefix(session)
            reply(message, "$prefix\n\nNo blocklist patterns configured.")
            return
        }

        val patternList = patterns.joinToString("\n") { pattern ->
            val durationText = pattern.actionDurationHours?.let { " (${it}h)" } ?: ""
            "- ${pattern.pattern} -> ${pattern.action}$durationText [severity: ${pattern.severity}]"
        }

        val prefix = adminSessionService.formatReplyPrefix(session)
        reply(message, "$prefix\n\nBlocklist patterns:\n$patternList")
    }
}
