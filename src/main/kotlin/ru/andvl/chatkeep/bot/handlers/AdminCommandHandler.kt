package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatAdministrators
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.service.AdminService
import ru.andvl.chatkeep.domain.service.ChatService

@Component
class AdminCommandHandler(
    private val chatService: ChatService,
    private val adminService: AdminService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        onCommand("start", initialFilter = { it.chat is PrivateChat }) { message ->
            reply(
                message,
                """
                Chatkeep Bot

                I collect messages from group chats where I'm added.

                Commands:
                /mychats - List chats where you're admin
                /stats <chat_id> - Get statistics for a chat
                /enable <chat_id> - Enable message collection
                /disable <chat_id> - Disable message collection
                """.trimIndent()
            )
        }

        onCommand("mychats", initialFilter = { it.chat is PrivateChat }) { message ->
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return@onCommand

            val adminChats = withContext(Dispatchers.IO) {
                chatService.getAllChats().filter { settings ->
                    isUserAdminInChat(bot, userId, settings.chatId)
                }
            }

            if (adminChats.isEmpty()) {
                reply(message, "You are not an admin in any chats where I'm active.")
                return@onCommand
            }

            val chatList = adminChats.joinToString("\n") { chat ->
                val status = if (chat.collectionEnabled) "ON" else "OFF"
                "- ${chat.chatTitle ?: "Unknown"} (ID: ${chat.chatId}) [$status]"
            }

            reply(message, "Your admin chats:\n$chatList")
        }

        onCommand("stats", initialFilter = { it.chat is PrivateChat }) { message ->
            val textContent = message.content as? TextContent ?: return@onCommand
            val args = textContent.text.split(" ").drop(1)
            val chatId = args.firstOrNull()?.toLongOrNull()

            if (chatId == null) {
                reply(message, "Usage: /stats <chat_id>")
                return@onCommand
            }

            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return@onCommand

            val isAdmin = withContext(Dispatchers.IO) {
                isUserAdminInChat(bot, userId, chatId)
            }

            if (!isAdmin) {
                reply(message, "You are not an admin in this chat.")
                return@onCommand
            }

            val stats = withContext(Dispatchers.IO) {
                adminService.getStatistics(chatId)
            }

            if (stats == null) {
                reply(message, "Chat not found or I'm not added to it.")
                return@onCommand
            }

            val statusEmoji = if (stats.collectionEnabled) "ON" else "OFF"
            reply(
                message,
                """
                Statistics for: ${stats.chatTitle ?: "Unknown"}

                Total messages: ${stats.totalMessages}
                Unique users: ${stats.uniqueUsers}
                Collection: $statusEmoji
                """.trimIndent()
            )
        }

        onCommand("enable", initialFilter = { it.chat is PrivateChat }) { message ->
            handleToggleCollection(message, true)
        }

        onCommand("disable", initialFilter = { it.chat is PrivateChat }) { message ->
            handleToggleCollection(message, false)
        }
    }

    private suspend fun BehaviourContext.handleToggleCollection(
        message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>,
        enable: Boolean
    ) {
        val textContent = message.content as? TextContent ?: return
        val args = textContent.text.split(" ").drop(1)
        val chatId = args.firstOrNull()?.toLongOrNull()

        if (chatId == null) {
            reply(message, "Usage: /${if (enable) "enable" else "disable"} <chat_id>")
            return
        }

        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

        val isAdmin = withContext(Dispatchers.IO) {
            isUserAdminInChat(bot, userId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You are not an admin in this chat.")
            return
        }

        val result = withContext(Dispatchers.IO) {
            chatService.setCollectionEnabled(chatId, enable)
        }

        if (result == null) {
            reply(message, "Chat not found or I'm not added to it.")
            return
        }

        val action = if (enable) "enabled" else "disabled"
        reply(message, "Message collection $action for chat ${result.chatTitle ?: chatId}")
    }

    private suspend fun isUserAdminInChat(bot: TelegramBot, userId: Long, chatId: Long): Boolean {
        return try {
            val admins = bot.getChatAdministrators(ChatId(RawChatId(chatId)))
            admins.any { it.user.id.chatId.long == userId }
        } catch (e: Exception) {
            logger.debug("Failed to get admins for chat $chatId: ${e.message}")
            false
        }
    }
}
