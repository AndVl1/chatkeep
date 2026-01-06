package ru.andvl.chatkeep.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import ru.andvl.chatkeep.api.support.TestDataFactory
import ru.andvl.chatkeep.domain.model.channelreply.ChannelReplySettings

/**
 * Integration tests for MiniAppChannelReplyController.
 * Tests channel reply settings retrieval and updates.
 */
class MiniAppChannelReplyControllerTest : MiniAppApiTestBase() {

    @Test
    fun `GET channel-reply - returns 401 when not authenticated`() {
        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `GET channel-reply - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET channel-reply - returns default settings when not configured`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(false) }
            jsonPath("$.replyText") { doesNotExist() }
            jsonPath("$.mediaFileId") { doesNotExist() }
            jsonPath("$.buttons") { isArray() }
            jsonPath("$.buttons.length()") { value(0) }
        }
    }

    @Test
    fun `GET channel-reply - returns existing settings`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        channelReplySettingsRepository.save(
            ChannelReplySettings(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                enabled = true,
                replyText = "Welcome to our channel!",
                mediaFileId = "AgACAgIAAxUAAWZn...",
                buttonsJson = """[{"text":"Website","url":"https://example.com"}]"""
            )
        )

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
            jsonPath("$.replyText") { value("Welcome to our channel!") }
            jsonPath("$.mediaFileId") { value("AgACAgIAAxUAAWZn...") }
            jsonPath("$.buttons.length()") { value(1) }
            jsonPath("$.buttons[0].text") { value("Website") }
            jsonPath("$.buttons[0].url") { value("https://example.com") }
        }
    }

    @Test
    fun `GET channel-reply - handles null buttons JSON`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        channelReplySettingsRepository.save(
            ChannelReplySettings(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                enabled = true,
                replyText = "Hello",
                mediaFileId = null,
                buttonsJson = null
            )
        )

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$.buttons") { isArray() }
            jsonPath("$.buttons.length()") { value(0) }
        }
    }

    @Test
    fun `PUT channel-reply - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": true}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `PUT channel-reply - validates reply text length`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Text too long (>4096 chars)
        val longText = "a".repeat(4097)

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"replyText": "$longText"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT channel-reply - validates button URL format`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Invalid URL (not https://, http://, or tg://)
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "buttons": [
                        {"text": "Invalid", "url": "ftp://example.com"}
                    ]
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT channel-reply - enables channel reply`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": true}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
        }

        // Verify settings saved
        val settings = channelReplySettingsRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(settings?.enabled).isTrue()
    }

    @Test
    fun `PUT channel-reply - disables channel reply`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // First enable it
        channelReplySettingsRepository.save(
            ChannelReplySettings(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                enabled = true
            )
        )

        // Then disable it
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": false}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(false) }
        }

        // Verify settings updated
        val settings = channelReplySettingsRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(settings?.enabled).isFalse()
    }

    @Test
    fun `PUT channel-reply - updates reply text`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"replyText": "Welcome to our channel!"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.replyText") { value("Welcome to our channel!") }
        }

        // Verify text saved
        val settings = channelReplySettingsRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(settings?.replyText).isEqualTo("Welcome to our channel!")
    }

    @Test
    fun `PUT channel-reply - updates buttons with https URL`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "buttons": [
                        {"text": "Website", "url": "https://example.com"},
                        {"text": "Channel", "url": "https://t.me/channel"}
                    ]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.buttons.length()") { value(2) }
            jsonPath("$.buttons[0].text") { value("Website") }
            jsonPath("$.buttons[0].url") { value("https://example.com") }
            jsonPath("$.buttons[1].text") { value("Channel") }
            jsonPath("$.buttons[1].url") { value("https://t.me/channel") }
        }
    }

    @Test
    fun `PUT channel-reply - updates buttons with tg URL`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "buttons": [
                        {"text": "Bot", "url": "tg://resolve?domain=botusername"}
                    ]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.buttons[0].url") { value("tg://resolve?domain=botusername") }
        }
    }

    @Test
    fun `PUT channel-reply - updates multiple settings at once`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "enabled": true,
                    "replyText": "Welcome!",
                    "buttons": [
                        {"text": "Join", "url": "https://t.me/channel"}
                    ]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
            jsonPath("$.replyText") { value("Welcome!") }
            jsonPath("$.buttons.length()") { value(1) }
        }

        // Verify all settings saved
        val settings = channelReplySettingsRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(settings?.enabled).isTrue()
        assertThat(settings?.replyText).isEqualTo("Welcome!")
        assertThat(settings?.buttonsJson).isNotNull()
    }

    @Test
    fun `PUT channel-reply - clears buttons by passing empty array`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // First set some buttons
        channelReplySettingsRepository.save(
            ChannelReplySettings(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                buttonsJson = """[{"text":"Old","url":"https://old.com"}]"""
            )
        )

        // Clear them
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"buttons": []}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.buttons.length()") { value(0) }
        }
    }

    @Test
    fun `PUT channel-reply - preserves unmodified fields`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Set initial state
        channelReplySettingsRepository.save(
            ChannelReplySettings(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                enabled = true,
                replyText = "Original text",
                buttonsJson = """[{"text":"Button","url":"https://example.com"}]"""
            )
        )

        // Update only enabled status
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": false}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(false) }
            jsonPath("$.replyText") { value("Original text") }
            jsonPath("$.buttons.length()") { value(1) }
        }
    }

    @Test
    fun `PUT channel-reply - handles maximum text length`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Exactly 4096 chars (max allowed)
        val maxText = "a".repeat(4096)

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"replyText": "$maxText"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.replyText") { value(maxText) }
        }
    }

    @Test
    fun `PUT channel-reply - allows http URLs in buttons`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "buttons": [
                        {"text": "HTTP Link", "url": "http://example.com"}
                    ]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.buttons[0].url") { value("http://example.com") }
        }
    }

    @Test
    fun `PUT channel-reply - handles special characters in text`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        val textWithSpecialChars = """Welcome! 👋
        |
        |Join our channel for:
        |• News & updates
        |• Community support
        |
        |Visit: https://example.com""".trimMargin()

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/channel-reply") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"replyText": ${com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(textWithSpecialChars)}}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.replyText") { value(textWithSpecialChars) }
        }
    }
}
