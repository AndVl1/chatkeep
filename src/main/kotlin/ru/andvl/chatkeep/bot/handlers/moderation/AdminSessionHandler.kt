package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
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
class AdminSessionHandler(
    private val adminSessionService: AdminSessionService,
    private val adminCacheService: AdminCacheService,
    private val adminService: AdminService,
    private val chatService: ChatService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CHATS_PER_PAGE = 6 // 2 columns × 3 rows
        private const val COLUMNS = 2
        private const val MAX_TITLE_LENGTH = 20
    }

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

        // Build keyboard for current page
        val totalPages = (adminChats.size + CHATS_PER_PAGE - 1) / CHATS_PER_PAGE
        val currentPage = page.coerceIn(0, totalPages - 1)
        val startIndex = currentPage * CHATS_PER_PAGE
        val endIndex = minOf(startIndex + CHATS_PER_PAGE, adminChats.size)
        val chatsOnPage = adminChats.subList(startIndex, endIndex)

        val keyboard = buildChatSelectionKeyboard(chatsOnPage, currentPage, totalPages)

        send(
            message.chat,
            "Select a chat to connect:",
            replyMarkup = keyboard
        )
        logger.info("/connect: Showing chat selection to user $userId (page $currentPage of $totalPages)")
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

        val commandsHelp = """
            Connected to: ${stats.chatTitle ?: "Chat $chatId"}

            Available commands:
            • /addblock <pattern> {action} - Add blocklist pattern
            • /delblock <pattern> - Remove pattern
            • /blocklist - List patterns
            • /cleanservice <on|off> - Service message cleanup
            • /import_rose - Import Rose Bot settings (attach file)

            Channel auto-reply:
            • /setreply <text> - Set reply text
            • /replymedia - Reply to media to set it
            • /replybutton <text> <url> - Add URL button
            • /showreply - Show current settings
            • /replyenable / /replydisable - Toggle
            • /clearmedia - Remove media
            • /clearbuttons - Remove all buttons
            • /delreply - Delete all settings

            • /disconnect - Disconnect from chat
        """.trimIndent()

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
