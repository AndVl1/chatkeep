package ru.andvl.chatkeep.api

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.MediaType
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import ru.andvl.chatkeep.api.support.TestDataFactory
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.MatchType
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType

/**
 * Integration tests for MiniAppBlocklistController.
 * Tests blocklist pattern management (add, list, delete).
 */
class MiniAppBlocklistControllerTest : MiniAppApiTestBase() {

    @Test
    fun `GET blocklist - returns 401 when not authenticated`() {
        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `GET blocklist - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET blocklist - returns empty list when no patterns exist`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `GET blocklist - returns existing patterns`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Add patterns via service
        val pattern1 = testDataFactory.createBlocklistPattern(
            chatId = TestDataFactory.DEFAULT_CHAT_ID,
            pattern = "spam",
            matchType = MatchType.EXACT.name
        )
        val pattern2 = testDataFactory.createBlocklistPattern(
            chatId = TestDataFactory.DEFAULT_CHAT_ID,
            pattern = "test*",
            matchType = MatchType.WILDCARD.name
        )

        blocklistPatternRepository.save(pattern1)
        blocklistPatternRepository.save(pattern2)

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].pattern") { exists() }
            jsonPath("$[0].matchType") { exists() }
            jsonPath("$[0].action") { exists() }
            jsonPath("$[0].severity") { exists() }
        }
    }

    @Test
    fun `POST blocklist - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": "spam"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST blocklist - validates empty pattern`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": ""}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST blocklist - validates pattern length`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Pattern too long (>500 chars)
        val longPattern = "a".repeat(501)

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": "$longPattern"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST blocklist - validates invalid match type`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": "spam", "matchType": "INVALID"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST blocklist - validates invalid action`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": "spam", "action": "INVALID"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST blocklist - validates severity range`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Too low
        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": "spam", "severity": 0}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isBadRequest() }
        }

        // Too high
        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": "spam", "severity": 11}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST blocklist - creates pattern with auto-detected EXACT match type`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": "spam"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isCreated() }
            jsonPath("$.pattern") { value("spam") }
            jsonPath("$.matchType") { value(MatchType.EXACT.name) }
            jsonPath("$.action") { exists() }
        }

        // Verify pattern saved
        val patterns = blocklistPatternRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(patterns).hasSize(1)
        assertThat(patterns[0].pattern).isEqualTo("spam")
        assertThat(patterns[0].matchType).isEqualTo(MatchType.EXACT.name)
    }

    @Test
    fun `POST blocklist - creates pattern with auto-detected WILDCARD match type`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": "spam*"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isCreated() }
            jsonPath("$.pattern") { value("spam*") }
            jsonPath("$.matchType") { value(MatchType.WILDCARD.name) }
        }
    }

    @Test
    fun `POST blocklist - creates pattern with explicit match type and action`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "pattern": "badword",
                    "matchType": "EXACT",
                    "action": "WARN",
                    "severity": 7
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isCreated() }
            jsonPath("$.pattern") { value("badword") }
            jsonPath("$.matchType") { value("EXACT") }
            jsonPath("$.action") { value("WARN") }
            jsonPath("$.severity") { value(7) }
        }
    }

    @Test
    fun `POST blocklist - uses default action from config`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        moderationConfigRepository.save(
            ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                defaultBlocklistAction = PunishmentType.MUTE.name
            )
        )

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"pattern": "test"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isCreated() }
            jsonPath("$.action") { value(PunishmentType.MUTE.name) }
        }
    }

    @Test
    fun `DELETE blocklist - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.delete("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist/1") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `DELETE blocklist - returns 404 when pattern not found`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.delete("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist/99999") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `DELETE blocklist - deletes pattern successfully`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Create pattern
        val pattern = testDataFactory.createBlocklistPattern(
            chatId = TestDataFactory.DEFAULT_CHAT_ID,
            pattern = "spam"
        )
        val saved = blocklistPatternRepository.save(pattern)

        mockMvc.delete("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist/${saved.id}") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isNoContent() }
        }

        // Verify pattern deleted
        val patterns = blocklistPatternRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(patterns).isEmpty()
    }

    @Test
    fun `DELETE blocklist - cannot delete pattern from different chat`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        chatSettingsRepository.save(
            testDataFactory.createChatSettings(chatId = TestDataFactory.SECONDARY_CHAT_ID)
        )

        // Create pattern in different chat
        val pattern = testDataFactory.createBlocklistPattern(
            chatId = TestDataFactory.SECONDARY_CHAT_ID,
            pattern = "spam"
        )
        val saved = blocklistPatternRepository.save(pattern)

        mockMvc.delete("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist/${saved.id}") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isNotFound() }
        }

        // Verify pattern NOT deleted
        val patterns = blocklistPatternRepository.findByChatId(TestDataFactory.SECONDARY_CHAT_ID)
        assertThat(patterns).hasSize(1)
    }

    @Test
    fun `DELETE blocklist - logs to admin channel when log channel is configured`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings(chatTitle = "Test Chat"))

        // Setup log channel
        val logChannelId = -1001111111111L
        moderationConfigRepository.save(
            ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = logChannelId
            )
        )

        // Create pattern
        val pattern = testDataFactory.createBlocklistPattern(
            chatId = TestDataFactory.DEFAULT_CHAT_ID,
            pattern = "badword",
            matchType = MatchType.EXACT.name
        )
        val saved = blocklistPatternRepository.save(pattern)

        mockMvc.delete("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/blocklist/${saved.id}") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isNoContent() }
        }

        // Verify log entry was sent
        val logEntry = capturingLogChannelPort.waitForEntry(
            channelId = logChannelId,
            actionType = ActionType.BLOCKLIST_REMOVED,
            timeoutMs = 2000
        )

        assertThat(logEntry).isNotNull
        assertThat(logEntry?.chatId).isEqualTo(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(logEntry?.chatTitle).isEqualTo("Test Chat")
        assertThat(logEntry?.adminId).isEqualTo(user.id)
        assertThat(logEntry?.actionType).isEqualTo(ActionType.BLOCKLIST_REMOVED)
        assertThat(logEntry?.reason).contains("badword")
        Unit
    }
}
