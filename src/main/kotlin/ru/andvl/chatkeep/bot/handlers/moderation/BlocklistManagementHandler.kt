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
            handleAddBlock(message)
        }

        onCommand("delblock", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            handleDelBlock(message)
        }

        onCommand("blocklist", initialFilter = privateFilter) { message ->
            handleBlocklist(message)
        }
    }

    private suspend fun BehaviourContext.handleAddBlock(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        try {
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

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
            val fullText = textContent?.text?.substringAfter("/addblock")?.trim() ?: ""

            if (fullText.isEmpty()) {
                reply(
                    message,
                    """
                    Usage: /addblock <pattern> {action duration}

                    Examples:
                    /addblock spam
                    /addblock *spam* {warn}
                    /addblock badword {mute 1h}
                    /addblock scam {ban}

                    Actions: nothing, warn, mute, ban, kick
                    Duration (inside braces): 1h, 24h, 7d
                    """.trimIndent()
                )
                return
            }

            // Parse pattern and optional {action duration} block
            val braceMatch = Regex("""\{([^}]+)\}""").find(fullText)
            val pattern: String
            val action: PunishmentType
            var duration: Int? = null

            if (braceMatch != null) {
                pattern = fullText.substring(0, braceMatch.range.first).trim()
                val braceContent = braceMatch.groupValues[1].trim().split(Regex("\\s+"))
                val actionStr = braceContent.firstOrNull()?.uppercase() ?: ""

                action = try {
                    PunishmentType.valueOf(actionStr)
                } catch (e: Exception) {
                    val sessionPrefix = adminSessionService.formatReplyPrefix(session)
                    reply(message, "$sessionPrefix\n\nUnknown action: $actionStr\nValid actions: nothing, warn, mute, ban, kick")
                    return
                }

                if (braceContent.size > 1) {
                    duration = ru.andvl.chatkeep.bot.util.DurationParser.parse(braceContent[1])?.let {
                        ru.andvl.chatkeep.bot.util.DurationParser.toHours(it)
                    }
                }
            } else {
                pattern = fullText.trim()
                action = withContext(Dispatchers.IO) {
                    blocklistService.getDefaultAction(chatId)
                }
            }

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

            val matchType = if (pattern.contains("*") || pattern.contains("?")) {
                MatchType.WILDCARD
            } else {
                MatchType.EXACT
            }

            withContext(Dispatchers.IO) {
                blocklistService.addPattern(chatId, pattern, matchType, action, duration, 0)
            }

            val prefix = adminSessionService.formatReplyPrefix(session)
            reply(message, "$prefix\n\nAdded blocklist pattern:\nPattern: $pattern\nAction: $action")
            logger.info("Blocklist pattern added: chatId=$chatId, pattern='$pattern', action=$action")
        } catch (e: Exception) {
            logger.error("/addblock: Failed", e)
            reply(message, "An error occurred. Please try again.")
        }
    }

    private suspend fun BehaviourContext.handleDelBlock(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

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
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

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
