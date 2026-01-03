package ru.andvl.chatkeep.bot.util

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.ChatSettings
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for keyboard utility functions.
 *
 * Tests the keyboard building and title truncation logic from
 * AdminSessionHandler and ConnectCallbackHandler.
 *
 * Note: These tests validate the logic that exists in the handlers.
 * The functions are duplicated here for testing purposes.
 */
class KeyboardUtilsTest {

    companion object {
        private const val MAX_TITLE_LENGTH = 20
    }

    @Nested
    inner class TruncateTitleTests {

        @Test
        fun `truncateTitle with short title returns unchanged`() {
            // Given
            val title = "Test Chat"

            // When
            val result = truncateTitle(title)

            // Then
            assertEquals("Test Chat", result)
        }

        @Test
        fun `truncateTitle with long title truncates with ellipsis`() {
            // Given
            val title = "This is a very long chat title that exceeds the limit"

            // When
            val result = truncateTitle(title)

            // Then
            assertEquals(MAX_TITLE_LENGTH, result.length)
            assertTrue(result.endsWith("..."))
            assertEquals("This is a very lo...", result)
        }

        @Test
        fun `truncateTitle with title exactly at limit returns unchanged`() {
            // Given
            val title = "12345678901234567890" // Exactly 20 characters

            // When
            val result = truncateTitle(title)

            // Then
            assertEquals(title, result)
            assertEquals(MAX_TITLE_LENGTH, result.length)
        }

        @Test
        fun `truncateTitle with title one char over limit truncates`() {
            // Given
            val title = "123456789012345678901" // 21 characters

            // When
            val result = truncateTitle(title)

            // Then
            assertEquals(MAX_TITLE_LENGTH, result.length)
            assertTrue(result.endsWith("..."))
            assertEquals("12345678901234567...", result)
        }

        @Test
        fun `truncateTitle with empty string returns empty`() {
            // Given
            val title = ""

            // When
            val result = truncateTitle(title)

            // Then
            assertEquals("", result)
        }

        @Test
        fun `truncateTitle with unicode characters truncates correctly`() {
            // Given
            val title = "Чат с очень длинным названием для тестирования"

            // When
            val result = truncateTitle(title)

            // Then
            assertEquals(MAX_TITLE_LENGTH, result.length)
            assertTrue(result.endsWith("..."))
        }

        @Test
        fun `truncateTitle with emojis truncates correctly`() {
            // Given
            val title = "🔥 Chat with lots of emojis 🎉🎊🎈"

            // When
            val result = truncateTitle(title)

            // Then
            assertEquals(MAX_TITLE_LENGTH, result.length)
            assertTrue(result.endsWith("..."))
        }
    }

    @Nested
    inner class BuildChatSelectionKeyboardTests {

        @Test
        fun `buildChatSelectionKeyboard with single page has no navigation`() {
            // Given - 3 chats fit on one page (< 6)
            val chats = listOf(
                createChatSettings(1, "Chat 1"),
                createChatSettings(2, "Chat 2"),
                createChatSettings(3, "Chat 3")
            )

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 1)

            // Then
            assertEquals(2, keyboard.keyboard.size) // 2 rows for 3 chats (2 columns)

            // First row should have 2 chat buttons
            assertEquals(2, keyboard.keyboard[0].size)
            assertChatButton(keyboard.keyboard[0][0], "Chat 1", "connect_sel:1")
            assertChatButton(keyboard.keyboard[0][1], "Chat 2", "connect_sel:2")

            // Second row should have 1 chat button
            assertEquals(1, keyboard.keyboard[1].size)
            assertChatButton(keyboard.keyboard[1][0], "Chat 3", "connect_sel:3")
        }

        @Test
        fun `buildChatSelectionKeyboard with multiple pages has navigation`() {
            // Given - 6 chats on page 1 of 2
            val chats = (1..6).map { createChatSettings(it.toLong(), "Chat $it") }

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 2)

            // Then - 3 rows of chats + 1 navigation row
            assertEquals(4, keyboard.keyboard.size)

            // Last row should be navigation
            val navRow = keyboard.keyboard[3]
            assertTrue(navRow.size >= 2) // Should have at least page indicator and next button

            // Should have next button (no back on first page)
            val nextButton = navRow.find { it is CallbackDataInlineKeyboardButton && it.callbackData == "connect_page:1" }
            assertTrue(nextButton != null, "Should have next button on first page")

            // Should have page indicator
            val pageIndicator = navRow.find { it is CallbackDataInlineKeyboardButton && it.text == "1 / 2" }
            assertTrue(pageIndicator != null, "Should have page indicator")
        }

        @Test
        fun `buildChatSelectionKeyboard on first page has no back button`() {
            // Given
            val chats = (1..6).map { createChatSettings(it.toLong(), "Chat $it") }

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 2)

            // Then
            val navRow = keyboard.keyboard.last()
            val backButton = navRow.find { it is CallbackDataInlineKeyboardButton && it.text == "◀️" }
            assertTrue(backButton == null, "Should NOT have back button on first page")
        }

        @Test
        fun `buildChatSelectionKeyboard on last page has no next button`() {
            // Given
            val chats = (1..6).map { createChatSettings(it.toLong(), "Chat $it") }

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 1, totalPages = 2)

            // Then
            val navRow = keyboard.keyboard.last()
            val nextButton = navRow.find { it is CallbackDataInlineKeyboardButton && it.text == "▶️" }
            assertTrue(nextButton == null, "Should NOT have next button on last page")
        }

        @Test
        fun `buildChatSelectionKeyboard on middle page has both navigation buttons`() {
            // Given
            val chats = (1..6).map { createChatSettings(it.toLong(), "Chat $it") }

            // When - on middle page (page 1 of 0,1,2)
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 1, totalPages = 3)

            // Then
            val navRow = keyboard.keyboard.last()
            val backButton = navRow.find { it is CallbackDataInlineKeyboardButton && it.text == "◀️" }
            val nextButton = navRow.find { it is CallbackDataInlineKeyboardButton && it.text == "▶️" }

            assertTrue(backButton != null, "Should have back button on middle page")
            assertTrue(nextButton != null, "Should have next button on middle page")
        }

        @Test
        fun `buildChatSelectionKeyboard with empty list creates empty keyboard`() {
            // Given
            val chats = emptyList<ChatSettings>()

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 1)

            // Then
            assertEquals(0, keyboard.keyboard.size)
        }

        @Test
        fun `buildChatSelectionKeyboard with single chat creates single button`() {
            // Given
            val chats = listOf(createChatSettings(1, "Single Chat"))

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 1)

            // Then
            assertEquals(1, keyboard.keyboard.size)
            assertEquals(1, keyboard.keyboard[0].size)
            assertChatButton(keyboard.keyboard[0][0], "Single Chat", "connect_sel:1")
        }

        @Test
        fun `buildChatSelectionKeyboard with two chats creates single row`() {
            // Given
            val chats = listOf(
                createChatSettings(1, "Chat 1"),
                createChatSettings(2, "Chat 2")
            )

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 1)

            // Then
            assertEquals(1, keyboard.keyboard.size) // Single row
            assertEquals(2, keyboard.keyboard[0].size) // Two buttons
            assertChatButton(keyboard.keyboard[0][0], "Chat 1", "connect_sel:1")
            assertChatButton(keyboard.keyboard[0][1], "Chat 2", "connect_sel:2")
        }

        @Test
        fun `buildChatSelectionKeyboard with odd number of chats fills rows correctly`() {
            // Given - 5 chats
            val chats = (1..5).map { createChatSettings(it.toLong(), "Chat $it") }

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 1)

            // Then
            assertEquals(3, keyboard.keyboard.size) // 3 rows: 2 + 2 + 1

            // First row: 2 chats
            assertEquals(2, keyboard.keyboard[0].size)
            // Second row: 2 chats
            assertEquals(2, keyboard.keyboard[1].size)
            // Third row: 1 chat
            assertEquals(1, keyboard.keyboard[2].size)
        }

        @Test
        fun `buildChatSelectionKeyboard with null title uses fallback`() {
            // Given
            val chats = listOf(createChatSettings(123, null))

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 1)

            // Then
            assertEquals(1, keyboard.keyboard.size)
            assertChatButton(keyboard.keyboard[0][0], "Chat 123", "connect_sel:123")
        }

        @Test
        fun `buildChatSelectionKeyboard truncates long titles`() {
            // Given
            val longTitle = "This is a very long chat title that needs truncation"
            val chats = listOf(createChatSettings(1, longTitle))

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 1)

            // Then
            val button = keyboard.keyboard[0][0] as CallbackDataInlineKeyboardButton
            assertEquals(MAX_TITLE_LENGTH, button.text.length)
            assertTrue(button.text.endsWith("..."))
        }

        @Test
        fun `buildChatSelectionKeyboard page indicator shows correct format`() {
            // Given
            val chats = (1..6).map { createChatSettings(it.toLong(), "Chat $it") }

            // When
            val keyboardPage1 = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 3)
            val keyboardPage2 = buildChatSelectionKeyboard(chats, currentPage = 1, totalPages = 3)
            val keyboardPage3 = buildChatSelectionKeyboard(chats, currentPage = 2, totalPages = 3)

            // Then
            val page1Indicator = keyboardPage1.keyboard.last().find {
                it is CallbackDataInlineKeyboardButton && it.text.contains("/")
            } as CallbackDataInlineKeyboardButton
            assertEquals("1 / 3", page1Indicator.text)

            val page2Indicator = keyboardPage2.keyboard.last().find {
                it is CallbackDataInlineKeyboardButton && it.text.contains("/")
            } as CallbackDataInlineKeyboardButton
            assertEquals("2 / 3", page2Indicator.text)

            val page3Indicator = keyboardPage3.keyboard.last().find {
                it is CallbackDataInlineKeyboardButton && it.text.contains("/")
            } as CallbackDataInlineKeyboardButton
            assertEquals("3 / 3", page3Indicator.text)
        }

        @Test
        fun `buildChatSelectionKeyboard with exactly 6 chats fills single page perfectly`() {
            // Given - exactly 6 chats (CHATS_PER_PAGE)
            val chats = (1..6).map { createChatSettings(it.toLong(), "Chat $it") }

            // When
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 0, totalPages = 1)

            // Then
            assertEquals(3, keyboard.keyboard.size) // 3 rows of 2 columns each
            keyboard.keyboard.forEach { row ->
                assertEquals(2, row.size) // Each row should have exactly 2 buttons
            }
        }

        @Test
        fun `buildChatSelectionKeyboard navigation buttons have correct callbacks`() {
            // Given
            val chats = (1..6).map { createChatSettings(it.toLong(), "Chat $it") }

            // When - on page 1 of 3
            val keyboard = buildChatSelectionKeyboard(chats, currentPage = 1, totalPages = 3)

            // Then
            val navRow = keyboard.keyboard.last()
            val backButton = navRow.find {
                it is CallbackDataInlineKeyboardButton && it.text == "◀️"
            } as? CallbackDataInlineKeyboardButton
            val nextButton = navRow.find {
                it is CallbackDataInlineKeyboardButton && it.text == "▶️"
            } as? CallbackDataInlineKeyboardButton

            assertEquals("connect_page:0", backButton?.callbackData)
            assertEquals("connect_page:2", nextButton?.callbackData)
        }
    }

    // Helper functions - duplicated from handlers for testing
    private fun truncateTitle(title: String): String {
        return if (title.length > MAX_TITLE_LENGTH) {
            title.take(MAX_TITLE_LENGTH - 3) + "..."
        } else {
            title
        }
    }

    private fun buildChatSelectionKeyboard(
        chats: List<ChatSettings>,
        currentPage: Int,
        totalPages: Int
    ): InlineKeyboardMarkup {
        val rows = mutableListOf<List<CallbackDataInlineKeyboardButton>>()

        // Add chat buttons in 2 columns
        var i = 0
        while (i < chats.size) {
            val rowButtons = mutableListOf<CallbackDataInlineKeyboardButton>()

            // First column
            val chat1 = chats[i]
            val title1 = truncateTitle(chat1.chatTitle ?: "Chat ${chat1.chatId}")
            rowButtons.add(CallbackDataInlineKeyboardButton(title1, "connect_sel:${chat1.chatId}"))

            // Second column (if exists)
            if (i + 1 < chats.size) {
                val chat2 = chats[i + 1]
                val title2 = truncateTitle(chat2.chatTitle ?: "Chat ${chat2.chatId}")
                rowButtons.add(CallbackDataInlineKeyboardButton(title2, "connect_sel:${chat2.chatId}"))
            }

            rows.add(rowButtons)
            i += 2
        }

        // Add pagination row if needed
        if (totalPages > 1) {
            val navButtons = mutableListOf<CallbackDataInlineKeyboardButton>()

            if (currentPage > 0) {
                navButtons.add(CallbackDataInlineKeyboardButton("◀️", "connect_page:${currentPage - 1}"))
            }
            navButtons.add(CallbackDataInlineKeyboardButton(
                "${currentPage + 1} / $totalPages",
                "connect_page:$currentPage"
            ))
            if (currentPage < totalPages - 1) {
                navButtons.add(CallbackDataInlineKeyboardButton("▶️", "connect_page:${currentPage + 1}"))
            }

            rows.add(navButtons)
        }

        return InlineKeyboardMarkup(rows)
    }

    private fun createChatSettings(chatId: Long, title: String?): ChatSettings {
        return ChatSettings(
            id = chatId,
            chatId = chatId,
            chatTitle = title,
            collectionEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun assertChatButton(
        button: dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton,
        expectedText: String,
        expectedCallback: String
    ) {
        assertTrue(button is CallbackDataInlineKeyboardButton)
        val callbackButton = button as CallbackDataInlineKeyboardButton
        assertEquals(expectedText, callbackButton.text)
        assertEquals(expectedCallback, callbackButton.callbackData)
    }
}
