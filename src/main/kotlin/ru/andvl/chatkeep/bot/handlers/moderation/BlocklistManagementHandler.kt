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
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: run {
            logger.warn("/addblock: Cannot extract user ID from message type ${message::class.simpleName}")
            return
        }

        val session = withContext(Dispatchers.IO) {
            adminSessionService.getSession(userId)
        } ?: run {
            logger.debug("/addblock: No session for userId=$userId")
            reply(message, "You must be connected to a chat first. Use /connect <chat_id>")
            return
        }

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
        val args = textContent?.text?.split(" ")?.drop(1) ?: emptyList()

        if (args.isEmpty()) {
            reply(
                message,
                """
                Usage: /addblock <pattern> [action] [duration] [severity]

                Example: /addblock *spam*
                Example: /addblock *spam* warn
                Example: /addblock badword mute 1h 5

                Actions: nothing, warn, mute, ban, kick
                - nothing: just delete message, no punishment
                - If action not specified, uses chat's default action

                Duration: 1h, 24h, 7d (optional)
                Severity: 0-10 (higher = priority, optional)
                """.trimIndent()
            )
            return
        }

        val pattern = args[0]

        // Validate pattern length
        if (pattern.length > 100) {
            val sessionPrefix = adminSessionService.formatReplyPrefix(session)
            reply(message, "$sessionPrefix\n\nPattern too long. Maximum 100 characters.")
            return
        }
        if (pattern.isBlank()) {
            val sessionPrefix = adminSessionService.formatReplyPrefix(session)
            reply(message, "$sessionPrefix\n\nPattern cannot be empty.")
            return
        }

        // Action is optional - use default from config if not specified
        val action: PunishmentType
        val durationArgIndex: Int
        val severityArgIndex: Int

        if (args.size > 1) {
            // Check if second arg is an action or a duration/severity
            val possibleAction = args[1].uppercase()
            val parsedAction = try {
                PunishmentType.valueOf(possibleAction)
            } catch (e: Exception) {
                null
            }

            if (parsedAction != null) {
                action = parsedAction
                durationArgIndex = 2
                severityArgIndex = 3
            } else {
                // Second arg is not an action, use default
                action = withContext(Dispatchers.IO) {
                    blocklistService.getDefaultAction(chatId)
                }
                durationArgIndex = 1
                severityArgIndex = 2
            }
        } else {
            // Only pattern provided, use default action
            action = withContext(Dispatchers.IO) {
                blocklistService.getDefaultAction(chatId)
            }
            durationArgIndex = -1
            severityArgIndex = -1
        }

        val duration = if (args.size > durationArgIndex && durationArgIndex >= 0) {
            ru.andvl.chatkeep.bot.util.DurationParser.parse(args[durationArgIndex])?.let {
                ru.andvl.chatkeep.bot.util.DurationParser.toHours(it)
            }
        } else null

        val severity = if (args.size > severityArgIndex && severityArgIndex >= 0) {
            args[severityArgIndex].toIntOrNull() ?: 0
        } else 0

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
