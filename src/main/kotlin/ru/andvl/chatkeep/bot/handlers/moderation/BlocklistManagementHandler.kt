package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.expandableBlockquote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.bot.util.AddBlockParser
import ru.andvl.chatkeep.bot.util.SessionAuthHelper
import ru.andvl.chatkeep.domain.model.moderation.MatchType
import ru.andvl.chatkeep.domain.service.moderation.BlocklistService

@Component
class BlocklistManagementHandler(
    private val blocklistService: BlocklistService,
    private val sessionAuthHelper: SessionAuthHelper
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        // Telegram message limit is 4096 chars, leave buffer for prefix
        private const val MAX_MESSAGE_LENGTH = 3800
    }

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
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            try {
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
                    return@withSessionAuth
                }

                // Parse pattern and optional {action duration} block using AddBlockParser
                val parseResult = AddBlockParser.parse(fullText)

                val pattern: String
                val action: ru.andvl.chatkeep.domain.model.moderation.PunishmentType
                val durationMinutes: Int?

                when (parseResult) {
                    is AddBlockParser.Result.Success -> {
                        val parsed = parseResult.parsed
                        pattern = parsed.pattern
                        durationMinutes = parsed.durationMinutes

                        // If action is null (no braces), use default from service
                        action = parsed.action ?: withContext(Dispatchers.IO) {
                            blocklistService.getDefaultAction(ctx.chatId)
                        }
                    }
                    is AddBlockParser.Result.Failure -> {
                        val sessionPrefix = sessionAuthHelper.formatReplyPrefix(ctx.session)
                        val errorMessage = when (val error = parseResult.error) {
                            is AddBlockParser.ParseError.EmptyInput -> "Pattern cannot be empty."
                            is AddBlockParser.ParseError.EmptyPattern -> "Pattern cannot be empty."
                            is AddBlockParser.ParseError.PatternTooLong -> "Pattern too long. Maximum ${error.maxLength} characters."
                            is AddBlockParser.ParseError.UnknownAction -> "Unknown action: ${error.actionStr}\nValid actions: nothing, warn, mute, ban, kick"
                        }
                        reply(message, "$sessionPrefix\n\n$errorMessage")
                        return@withSessionAuth
                    }
                }

                val matchType = if (pattern.contains("*") || pattern.contains("?")) {
                    MatchType.WILDCARD
                } else {
                    MatchType.EXACT
                }

                val result = withContext(Dispatchers.IO) {
                    blocklistService.addPattern(ctx.chatId, pattern, matchType, action, durationMinutes, 0)
                }

                val prefix = sessionAuthHelper.formatReplyPrefix(ctx.session)
                val operation = if (result.isUpdate) "Updated" else "Added"
                reply(message, "$prefix\n\n$operation blocklist pattern:\nPattern: $pattern\nAction: $action")
                logger.info("Blocklist pattern ${operation.lowercase()}: chatId=${ctx.chatId}, pattern='$pattern', action=$action")
            } catch (e: Exception) {
                logger.error("/addblock: Failed", e)
                reply(message, "An error occurred. Please try again.")
            }
        }
    }

    private suspend fun BehaviourContext.handleDelBlock(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val textContent = message.content as? TextContent
            val args = textContent?.text?.split(" ")?.drop(1) ?: emptyList()

            if (args.isEmpty()) {
                reply(message, "Usage: /delblock <pattern>")
                return@withSessionAuth
            }

            val pattern = args[0]

            withContext(Dispatchers.IO) {
                blocklistService.removePattern(ctx.chatId, pattern)
            }

            val prefix = sessionAuthHelper.formatReplyPrefix(ctx.session)
            reply(message, "$prefix\n\nRemoved blocklist pattern: $pattern")
            logger.info("Blocklist pattern removed: chatId=${ctx.chatId}, pattern='$pattern'")
        }
    }

    private suspend fun BehaviourContext.handleBlocklist(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val patterns = withContext(Dispatchers.IO) {
                blocklistService.listPatterns(ctx.chatId)
            }

            val prefix = sessionAuthHelper.formatReplyPrefix(ctx.session)

            if (patterns.isEmpty()) {
                reply(message, "$prefix\n\nNo blocklist patterns configured.")
                return@withSessionAuth
            }

            val patternList = patterns.joinToString("\n") { pattern ->
                val durationText = pattern.actionDurationMinutes?.let { " (${ru.andvl.chatkeep.bot.util.DurationParser.formatMinutes(it)})" } ?: ""
                "- ${pattern.pattern} -> ${pattern.action}$durationText [severity: ${pattern.severity}]"
            }

            val headerText = "$prefix\n\nBlocklist patterns (${patterns.size}):\n"
            val fullMessage = headerText + patternList

            if (fullMessage.length <= MAX_MESSAGE_LENGTH) {
                // Fits in one message - use expandable blockquote for better UX
                val entities = buildEntities {
                    +prefix
                    +"\n\nBlocklist patterns (${patterns.size}):\n"
                    expandableBlockquote(patternList)
                }
                sendTextMessage(message.chat, entities = entities)
            } else {
                // Too long - send as .txt file
                val fileContent = patternList.toByteArray(Charsets.UTF_8)
                val fileName = "blocklist_${ctx.chatId}.txt"
                sendDocument(
                    chatId = message.chat.id,
                    document = fileContent.asMultipartFile(fileName),
                    text = "$prefix\n\nBlocklist contains ${patterns.size} patterns (too many to display in message)."
                )
                logger.info("Sent blocklist as file for chatId=${ctx.chatId}, patterns=${patterns.size}")
            }
        }
    }
}
