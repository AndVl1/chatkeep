package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.AdminSessionService
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository

@Component
class CleanServiceCommandHandler(
    private val moderationConfigRepository: ModerationConfigRepository,
    private val adminSessionService: AdminSessionService,
    private val adminCacheService: AdminCacheService,
    private val logChannelService: LogChannelService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        onCommand("cleanservice", requireOnlyCommandInMessage = false) { message ->
            when (message.chat) {
                is GroupChat, is SupergroupChat -> handleGroupCommand(message)
                is PrivateChat -> handlePrivateCommand(message)
                else -> return@onCommand
            }
        }
    }

    private suspend fun BehaviourContext.handleGroupCommand(message: CommonMessage<*>) {
        val chatId = message.chat.id.chatId.long
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(userId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You must be an admin to use this command.")
            return
        }

        val textContent = message.content as? TextContent
        val args = textContent?.text?.split(" ")?.drop(1) ?: emptyList()

        handleCleanService(message, chatId, args, null)
    }

    private suspend fun BehaviourContext.handlePrivateCommand(message: CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

        val session = withContext(Dispatchers.IO) {
            adminSessionService.getSession(userId)
        }

        if (session == null) {
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

        val prefix = adminSessionService.formatReplyPrefix(session)
        handleCleanService(message, chatId, args, prefix)
    }

    private suspend fun BehaviourContext.handleCleanService(
        message: CommonMessage<*>,
        chatId: Long,
        args: List<String>,
        replyPrefix: String?
    ) {
        if (args.isEmpty()) {
            val config = withContext(Dispatchers.IO) {
                moderationConfigRepository.findByChatId(chatId)
            }

            val status = if (config?.cleanServiceEnabled == true) "ON" else "OFF"
            val response = buildString {
                if (replyPrefix != null) {
                    appendLine(replyPrefix)
                    appendLine()
                }
                appendLine("Clean service messages: $status")
                appendLine()
                appendLine("Usage: /cleanservice <on|off>")
                appendLine()
                appendLine("When enabled, the bot will automatically delete")
                appendLine("'user joined' and 'user left' service messages.")
            }
            reply(message, response)
            return
        }

        val arg = args[0].lowercase()
        val enabled = when (arg) {
            "on", "true", "yes", "1", "enable" -> true
            "off", "false", "no", "0", "disable" -> false
            else -> {
                val response = buildString {
                    if (replyPrefix != null) {
                        appendLine(replyPrefix)
                        appendLine()
                    }
                    appendLine("Invalid argument: $arg")
                    appendLine("Usage: /cleanservice <on|off>")
                }
                reply(message, response)
                return
            }
        }

        val updated = withContext(Dispatchers.IO) {
            moderationConfigRepository.updateCleanServiceEnabled(chatId, enabled)
        }

        if (updated > 0) {
            val status = if (enabled) "enabled" else "disabled"
            val response = buildString {
                if (replyPrefix != null) {
                    appendLine(replyPrefix)
                    appendLine()
                }
                append("Clean service messages $status.")
            }
            reply(message, response)
            logger.info("Clean service ${if (enabled) "enabled" else "disabled"} for chat $chatId")

            // Log to channel (logChannelService is already async, no need for withContext)
            val admin = (message as? FromUserMessage)?.from ?: return
            val chatTitle = when (val chat = message.chat) {
                is GroupChat -> chat.title
                is SupergroupChat -> chat.title
                else -> null
            }

            logChannelService.logModerationAction(
                ModerationLogEntry(
                    chatId = chatId,
                    chatTitle = chatTitle,
                    adminId = admin.id.chatId.long,
                    adminFirstName = admin.firstName,
                    adminLastName = admin.lastName,
                    adminUserName = admin.username?.withoutAt,
                    actionType = if (enabled) ActionType.CLEAN_SERVICE_ON else ActionType.CLEAN_SERVICE_OFF,
                    source = PunishmentSource.MANUAL
                )
            )
        } else {
            val response = buildString {
                if (replyPrefix != null) {
                    appendLine(replyPrefix)
                    appendLine()
                }
                append("Failed to update settings. Make sure the bot is added to the chat.")
            }
            reply(message, response)
        }
    }
}
