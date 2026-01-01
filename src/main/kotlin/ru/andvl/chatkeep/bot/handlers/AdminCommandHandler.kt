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
import ru.andvl.chatkeep.domain.model.ChatSettings
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

                Admin Commands (Private Chat):
                /mychats - List chats where you're admin
                /stats <chat_id> - Get statistics for a chat
                /enable <chat_id> - Enable message collection
                /disable <chat_id> - Disable message collection
                /connect <chat_id> - Connect to a chat for management
                /disconnect - Disconnect from current chat
                /addblock <pattern> <action> - Add blocklist pattern
                /delblock <pattern> - Remove blocklist pattern
                /blocklist - List blocklist patterns

                Moderation Commands (Group Chat):
                /warn - Warn a user (reply or user_id)
                /unwarn - Remove user warnings
                /mute [duration] - Mute a user (e.g. 1h, 24h)
                /unmute - Unmute a user
                /ban [duration] - Ban a user
                /unban - Unban a user
                /kick - Kick a user
                """.trimIndent()
            )
        }

        onCommand("mychats", initialFilter = { it.chat is PrivateChat }) { message ->
            logger.info("Received /mychats command from chat ${message.chat.id}")

            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long
            if (userId == null) {
                logger.warn("Could not get user ID from message")
                reply(message, "Could not identify you. Please try again.")
                return@onCommand
            }
            logger.info("/mychats: userId=$userId")

            val allChats = try {
                withContext(Dispatchers.IO) {
                    chatService.getAllChats()
                }
            } catch (e: Exception) {
                logger.error("/mychats: Failed to get chats from DB", e)
                reply(message, "Error loading chats. Please try again.")
                return@onCommand
            }
            logger.info("/mychats: Found ${allChats.size} chats in DB")

            if (allChats.isEmpty()) {
                logger.info("/mychats: No chats registered, sending response")
                reply(message, "I'm not added to any chats yet.")
                return@onCommand
            }

            // Filter chats where user is admin (must use loop for suspend calls)
            val adminChats = mutableListOf<ChatSettings>()
            for (settings in allChats) {
                logger.debug("/mychats: Checking admin status for chat ${settings.chatId}")
                try {
                    if (isUserAdminInChat(bot, userId, settings.chatId)) {
                        adminChats.add(settings)
                        logger.debug("/mychats: User is admin in chat ${settings.chatId}")
                    }
                } catch (e: Exception) {
                    logger.warn("/mychats: Error checking admin for chat ${settings.chatId}: ${e.message}")
                }
            }
            logger.info("/mychats: User is admin in ${adminChats.size} chats")

            if (adminChats.isEmpty()) {
                logger.info("/mychats: User is not admin in any chats, sending response")
                reply(message, "You are not an admin in any chats where I'm active.")
                return@onCommand
            }

            val chatList = adminChats.joinToString("\n") { chat ->
                val status = if (chat.collectionEnabled) "ON" else "OFF"
                "- ${chat.chatTitle ?: "Unknown"} (ID: ${chat.chatId}) [$status]"
            }

            logger.info("/mychats: Sending response with ${adminChats.size} chats")
            reply(message, "Your admin chats:\n$chatList")
            logger.info("/mychats: Response sent successfully")
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
