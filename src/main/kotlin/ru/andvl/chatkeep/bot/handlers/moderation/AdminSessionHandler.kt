package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
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
import ru.andvl.chatkeep.bot.util.KeyboardUtils
import ru.andvl.chatkeep.domain.service.AdminService
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.AdminSessionService

@Component
class AdminSessionHandler(
    private val adminSessionService: AdminSessionService,
    private val adminCacheService: AdminCacheService,
    private val adminService: AdminService,
    private val chatService: ChatService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        val privateFilter = { msg: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*> ->
            msg.chat is PrivateChat
        }

        onCommand("connect", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            handleConnect(message)
        }

        onCommand("disconnect", initialFilter = privateFilter) { message ->
            handleDisconnect(message)
        }
    }

    private suspend fun BehaviourContext.handleConnect(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        try {
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: run {
                logger.warn("/connect: Cannot extract user ID from message")
                return
            }

            val textContent = message.content as? TextContent
            val args = textContent?.text?.split(" ")?.drop(1) ?: emptyList()
            val chatId = args.firstOrNull()?.toLongOrNull()

            if (chatId == null) {
                // No chat_id provided - show interactive selection
                showChatSelectionKeyboard(message, userId)
                return
            }

            // Direct connection with chat_id
            connectToChat(message, userId, chatId)
        } catch (e: Exception) {
            logger.error("/connect: Failed for user in chat ${message.chat.id}", e)
            reply(message, "An error occurred. Please try again.")
        }
    }

    private suspend fun BehaviourContext.showChatSelectionKeyboard(
        message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>,
        userId: Long,
        page: Int = 0
    ) {
        // Get all chats from database
        val allChats = withContext(Dispatchers.IO) {
            chatService.getAllChats()
        }

        if (allChats.isEmpty()) {
            reply(message, "I'm not added to any chats yet.")
            return
        }

        // Filter chats where user is admin
        val adminChats = mutableListOf<ru.andvl.chatkeep.domain.model.ChatSettings>()
        for (settings in allChats) {
            try {
                if (withContext(Dispatchers.IO) {
                    adminCacheService.isAdmin(userId, settings.chatId)
                }) {
                    adminChats.add(settings)
                }
            } catch (e: Exception) {
                logger.warn("/connect: Error checking admin for chat ${settings.chatId}: ${e.message}")
            }
        }

        if (adminChats.isEmpty()) {
            reply(message, "You are not admin in any chats where I'm active.")
            return
        }

        // Build keyboard using shared utility
        val keyboard = KeyboardUtils.buildChatSelectionKeyboard(adminChats, page)

        send(
            message.chat,
            "Select a chat to connect:",
            replyMarkup = keyboard
        )
        logger.info("/connect: Showing chat selection to user $userId (page $page)")
    }

    private suspend fun BehaviourContext.connectToChat(
        message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>,
        userId: Long,
        chatId: Long
    ) {
        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(userId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You are not an admin in this chat.")
            return
        }

        val stats = withContext(Dispatchers.IO) {
            adminService.getStatistics(chatId)
        }

        if (stats == null) {
            reply(message, "Chat not found or I'm not added to it.")
            return
        }

        withContext(Dispatchers.IO) {
            adminSessionService.connect(userId, chatId, stats.chatTitle)
        }

        val commandsHelp = KeyboardUtils.getCommandsHelp(stats.chatTitle, chatId)

        reply(message, commandsHelp)
        logger.info("Admin session connected: userId=$userId, chatId=$chatId")
    }

    private suspend fun BehaviourContext.handleDisconnect(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

        val session = withContext(Dispatchers.IO) {
            adminSessionService.getSession(userId)
        }

        if (session == null) {
            reply(message, "You are not connected to any chat.")
            return
        }

        withContext(Dispatchers.IO) {
            adminSessionService.disconnect(userId)
        }

        reply(message, "Disconnected from: ${session.connectedChatTitle ?: "Chat ${session.connectedChatId}"}")
        logger.info("Admin session disconnected: userId=$userId")
    }
}
