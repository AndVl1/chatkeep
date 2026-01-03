package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
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

    companion object {
        private const val CHATS_PER_PAGE = 6
        private const val COLUMNS = 2
        private const val MAX_TITLE_LENGTH = 20
    }

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

            val commandsHelp = """
                Connected to: ${stats.chatTitle ?: "Chat $chatId"}

                Available commands:
                • /addblock <pattern> {action} - Add blocklist pattern
                • /delblock <pattern> - Remove pattern
                • /blocklist - List patterns
                • /cleanservice <on|off> - Service message cleanup
                • /import_rose - Import Rose Bot settings (attach file)
                • /disconnect - Disconnect from chat
            """.trimIndent()

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

            // Build keyboard for the requested page
            val totalPages = (adminChats.size + CHATS_PER_PAGE - 1) / CHATS_PER_PAGE
            val currentPage = page.coerceIn(0, totalPages - 1)
            val startIndex = currentPage * CHATS_PER_PAGE
            val endIndex = minOf(startIndex + CHATS_PER_PAGE, adminChats.size)
            val chatsOnPage = adminChats.subList(startIndex, endIndex)

            val keyboard = buildChatSelectionKeyboard(chatsOnPage, currentPage, totalPages)

            // Delete old message and send new one with updated page
            try {
                delete(query.message)
            } catch (e: Exception) {
                logger.warn("Failed to delete old selection message: ${e.message}")
            }

            try {
                send(query.message.chat, "Select a chat to connect:", replyMarkup = keyboard)
                answer(query)
                logger.info("Updated chat selection keyboard to page $currentPage for user $userId")
            } catch (e: Exception) {
                answer(query, "Failed to update page", showAlert = true)
                logger.error("Failed to send updated chat selection keyboard", e)
            }
        }
    }

    private fun buildChatSelectionKeyboard(
        chats: List<ru.andvl.chatkeep.domain.model.ChatSettings>,
        currentPage: Int,
        totalPages: Int
    ): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            keyboard = matrix {
                // Add chat buttons in 2 columns × 3 rows
                var i = 0
                while (i < chats.size) {
                    row {
                        // First column
                        val chat1 = chats[i]
                        val title1 = truncateTitle(chat1.chatTitle ?: "Chat ${chat1.chatId}")
                        +CallbackDataInlineKeyboardButton(
                            title1,
                            "connect_sel:${chat1.chatId}"
                        )

                        // Second column (if exists)
                        if (i + 1 < chats.size) {
                            val chat2 = chats[i + 1]
                            val title2 = truncateTitle(chat2.chatTitle ?: "Chat ${chat2.chatId}")
                            +CallbackDataInlineKeyboardButton(
                                title2,
                                "connect_sel:${chat2.chatId}"
                            )
                        }

                        i += COLUMNS
                    }
                }

                // Add pagination row if needed
                if (totalPages > 1) {
                    row {
                        if (currentPage > 0) {
                            +CallbackDataInlineKeyboardButton(
                                "◀️",
                                "connect_page:${currentPage - 1}"
                            )
                        }
                        +CallbackDataInlineKeyboardButton(
                            "${currentPage + 1} / $totalPages",
                            "connect_page:$currentPage" // Same page, no action
                        )
                        if (currentPage < totalPages - 1) {
                            +CallbackDataInlineKeyboardButton(
                                "▶️",
                                "connect_page:${currentPage + 1}"
                            )
                        }
                    }
                }
            }
        )
    }

    private fun truncateTitle(title: String): String {
        return if (title.length > MAX_TITLE_LENGTH) {
            title.take(MAX_TITLE_LENGTH - 3) + "..."
        } else {
            title
        }
    }
}
