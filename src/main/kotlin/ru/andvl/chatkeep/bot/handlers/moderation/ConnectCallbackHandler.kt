package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
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
class ConnectCallbackHandler(
    private val chatService: ChatService,
    private val adminSessionService: AdminSessionService,
    private val adminCacheService: AdminCacheService,
    private val adminService: AdminService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        // Handle chat selection: connect_sel:chatId
        onMessageDataCallbackQuery(Regex("connect_sel:.*")) { query ->
            val parts = query.data.split(":")
            if (parts.size != 2) {
                answer(query, "Invalid callback data", showAlert = true)
                logger.warn("Invalid connect_sel callback: ${query.data}")
                return@onMessageDataCallbackQuery
            }

            val chatId = parts[1].toLongOrNull()
            if (chatId == null) {
                answer(query, "Invalid chat ID", showAlert = true)
                logger.warn("Invalid chatId in connect_sel callback: ${query.data}")
                return@onMessageDataCallbackQuery
            }

            val userId = query.user.id.chatId.long

            // Verify user is admin
            val isAdmin = withContext(Dispatchers.IO) {
                adminCacheService.isAdmin(userId, chatId)
            }

            if (!isAdmin) {
                answer(query, "You are not admin in this chat", showAlert = true)
                return@onMessageDataCallbackQuery
            }

            // Get chat statistics
            val stats = withContext(Dispatchers.IO) {
                adminService.getStatistics(chatId)
            }

            if (stats == null) {
                answer(query, "Chat not found or I'm not added to it", showAlert = true)
                return@onMessageDataCallbackQuery
            }

            // Connect to chat
            withContext(Dispatchers.IO) {
                adminSessionService.connect(userId, chatId, stats.chatTitle)
            }

            val commandsHelp = KeyboardUtils.getCommandsHelp(stats.chatTitle, chatId)

            // Delete the selection message and send confirmation
            try {
                delete(query.message)
            } catch (e: Exception) {
                logger.warn("Failed to delete selection message: ${e.message}")
            }

            send(query.message.chat, commandsHelp)
            answer(query)
            logger.info("Admin session connected via callback: userId=$userId, chatId=$chatId")
        }

        // Handle pagination: connect_page:pageNumber
        onMessageDataCallbackQuery(Regex("connect_page:.*")) { query ->
            val parts = query.data.split(":")
            if (parts.size != 2) {
                answer(query, "Invalid callback data", showAlert = true)
                logger.warn("Invalid connect_page callback: ${query.data}")
                return@onMessageDataCallbackQuery
            }

            val page = parts[1].toIntOrNull()
            if (page == null) {
                answer(query, "Invalid page number", showAlert = true)
                logger.warn("Invalid page in connect_page callback: ${query.data}")
                return@onMessageDataCallbackQuery
            }

            val userId = query.user.id.chatId.long

            // Get all chats where user is admin
            val allChats = withContext(Dispatchers.IO) {
                chatService.getAllChats()
            }

            val adminChats = mutableListOf<ru.andvl.chatkeep.domain.model.ChatSettings>()
            for (settings in allChats) {
                try {
                    if (withContext(Dispatchers.IO) {
                        adminCacheService.isAdmin(userId, settings.chatId)
                    }) {
                        adminChats.add(settings)
                    }
                } catch (e: Exception) {
                    logger.warn("Error checking admin for chat ${settings.chatId}: ${e.message}")
                }
            }

            if (adminChats.isEmpty()) {
                answer(query, "You are not admin in any chats", showAlert = true)
                return@onMessageDataCallbackQuery
            }

            // Build keyboard for the requested page using shared utility
            val keyboard = KeyboardUtils.buildChatSelectionKeyboard(adminChats, page)

            // Use edit instead of delete+send to avoid flicker
            try {
                editMessageText(query.message.chat.id, query.message.messageId, "Select a chat to connect:", replyMarkup = keyboard)
                answer(query)
                logger.info("Updated chat selection keyboard to page $page for user $userId")
            } catch (e: Exception) {
                answer(query, "Failed to update page", showAlert = true)
                logger.error("Failed to edit chat selection keyboard", e)
            }
        }
    }

}
