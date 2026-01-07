package ru.andvl.chatkeep.api

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import ru.andvl.chatkeep.api.support.TestDataFactory

/**
 * Integration tests for MiniAppChatsController.
 * Tests chat listing and admin filtering.
 */
class MiniAppChatsControllerTest : MiniAppApiTestBase() {

    @Test
    fun `GET chats - returns 401 when not authenticated`() {
        mockMvc.get("/api/v1/miniapp/chats")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `GET chats - returns empty list when user has no admin chats`() {
        val user = testDataFactory.createTelegramUser(id = 999L)
        val authHeader = authTestHelper.createValidAuthHeader(user)

        // Create chats but user is not admin
        chatSettingsRepository.save(testDataFactory.createChatSettings(chatId = TestDataFactory.DEFAULT_CHAT_ID))
        chatSettingsRepository.save(testDataFactory.createChatSettings(chatId = TestDataFactory.SECONDARY_CHAT_ID))

        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, 999L)
        mockUserNotAdmin(TestDataFactory.SECONDARY_CHAT_ID, 999L)

        val result = mockMvc.get("/api/v1/miniapp/chats") {
            header("Authorization", authHeader)
        }

        println("Response status: ${result.andReturn().response.status}")
        println("Response body: ${result.andReturn().response.contentAsString}")

        result.andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `GET chats - returns user's admin chats`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        // Create test chats
        chatSettingsRepository.save(
            testDataFactory.createChatSettings(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                chatTitle = "Admin Chat"
            )
        )
        chatSettingsRepository.save(
            testDataFactory.createChatSettings(
                chatId = TestDataFactory.SECONDARY_CHAT_ID,
                chatTitle = "Non-Admin Chat"
            )
        )

        // User is admin only in DEFAULT_CHAT
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)
        mockUserNotAdmin(TestDataFactory.SECONDARY_CHAT_ID, user.id)

        mockMvc.get("/api/v1/miniapp/chats") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].chatId") { value(TestDataFactory.DEFAULT_CHAT_ID) }
            jsonPath("$[0].chatTitle") { value("Admin Chat") }
            jsonPath("$[0].memberCount") { doesNotExist() }
        }
    }

    @Test
    fun `GET chats - returns multiple admin chats`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(
            testDataFactory.createChatSettings(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                chatTitle = "First Chat"
            )
        )
        chatSettingsRepository.save(
            testDataFactory.createChatSettings(
                chatId = TestDataFactory.SECONDARY_CHAT_ID,
                chatTitle = "Second Chat"
            )
        )

        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)
        mockUserIsAdmin(TestDataFactory.SECONDARY_CHAT_ID, user.id)

        mockMvc.get("/api/v1/miniapp/chats") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
        }
    }

    @Test
    fun `GET chats - handles null chat title`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(
            testDataFactory.createChatSettings(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                chatTitle = null
            )
        )

        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.get("/api/v1/miniapp/chats") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].chatTitle") { doesNotExist() }
        }
    }

    @Test
    fun `GET chats - returns 401 with invalid token`() {
        mockMvc.get("/api/v1/miniapp/chats") {
            header("Authorization", authTestHelper.createInvalidAuthHeader())
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
