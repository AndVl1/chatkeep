package ru.andvl.chatkeep.bot.util

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import ru.andvl.chatkeep.domain.model.ChatSettings

/**
 * Utilities for building Telegram inline keyboards.
 */
object KeyboardUtils {
    private const val MAX_TITLE_LENGTH = 20
    private const val BUTTONS_PER_PAGE = 6
    private const val COLUMNS = 2

    /**
     * Truncates a chat title to fit in an inline keyboard button.
     * Uses ellipsis (…) when truncated.
     */
    fun truncateTitle(title: String?, chatId: Long): String {
        val displayTitle = title ?: "Chat $chatId"
        return if (displayTitle.length > MAX_TITLE_LENGTH) {
            displayTitle.take(MAX_TITLE_LENGTH - 1) + "…"
        } else {
            displayTitle
        }
    }

    /**
     * Builds a paginated inline keyboard for chat selection.
     * Layout: 2 columns × 3 rows with pagination controls.
     *
     * @param chats List of all chats to display (will be paginated)
     * @param page Current page number (0-based)
     * @return InlineKeyboardMarkup with chat buttons and pagination
     */
    fun buildChatSelectionKeyboard(
        chats: List<ChatSettings>,
        page: Int
    ): InlineKeyboardMarkup {
        val totalPages = (chats.size + BUTTONS_PER_PAGE - 1) / BUTTONS_PER_PAGE
        val safePage = page.coerceIn(0, maxOf(0, totalPages - 1))
        val startIndex = safePage * BUTTONS_PER_PAGE
        val pageChats = chats.drop(startIndex).take(BUTTONS_PER_PAGE)

        return InlineKeyboardMarkup(
            keyboard = matrix {
                // Chat buttons in 2-column grid
                pageChats.chunked(COLUMNS).forEach { rowChats ->
                    row {
                        rowChats.forEach { chat ->
                            +CallbackDataInlineKeyboardButton(
                                truncateTitle(chat.chatTitle, chat.chatId),
                                "connect_sel:${chat.chatId}"
                            )
                        }
                    }
                }
                // Pagination row
                if (totalPages > 1) {
                    row {
                        if (safePage > 0) {
                            +CallbackDataInlineKeyboardButton("◀️", "connect_page:${safePage - 1}")
                        }
                        +CallbackDataInlineKeyboardButton("${safePage + 1}/$totalPages", "connect_noop")
                        if (safePage < totalPages - 1) {
                            +CallbackDataInlineKeyboardButton("▶️", "connect_page:${safePage + 1}")
                        }
                    }
                }
            }
        )
    }

    /**
     * Returns the help text for available commands after connecting to a chat.
     * This ensures consistency across direct connect and callback handlers.
     */
    fun getCommandsHelp(chatTitle: String?, chatId: Long): String = """
        Connected to: ${chatTitle ?: "Chat $chatId"}

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
}
