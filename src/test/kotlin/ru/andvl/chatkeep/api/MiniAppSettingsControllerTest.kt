package ru.andvl.chatkeep.api

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.MediaType
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import ru.andvl.chatkeep.api.dto.UpdateSettingsRequest
import ru.andvl.chatkeep.api.support.TestDataFactory
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType

/**
 * Integration tests for MiniAppSettingsController.
 * Tests chat settings retrieval and updates with moderation config.
 */
class MiniAppSettingsControllerTest : MiniAppApiTestBase() {

    @Test
    fun `GET settings - returns 401 when not authenticated`() {
        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `GET settings - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET settings - returns 404 when chat not found`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(99999L, user.id)

        mockMvc.get("/api/v1/miniapp/chats/99999/settings") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET settings - returns default moderation config when not exists`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$.chatId") { value(TestDataFactory.DEFAULT_CHAT_ID) }
            jsonPath("$.collectionEnabled") { value(true) }
            jsonPath("$.cleanServiceEnabled") { value(false) }
            jsonPath("$.maxWarnings") { value(3) }
            jsonPath("$.warningTtlHours") { value(24) }
            jsonPath("$.thresholdAction") { value(PunishmentType.MUTE.name) } // Default is MUTE, not BAN
            jsonPath("$.defaultBlocklistAction") { value(PunishmentType.WARN.name) }
            jsonPath("$.lockWarnsEnabled") { value(false) } // Default is false when no lock settings exist
        }
    }

    @Test
    fun `GET settings - returns existing moderation config`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        moderationConfigRepository.save(
            ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                cleanServiceEnabled = true,
                maxWarnings = 5,
                warningTtlHours = 48,
                thresholdAction = PunishmentType.MUTE.name,
                thresholdDurationMinutes = 60,
                defaultBlocklistAction = PunishmentType.WARN.name,
                logChannelId = -1001234567890L
            )
        )

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$.cleanServiceEnabled") { value(true) }
            jsonPath("$.maxWarnings") { value(5) }
            jsonPath("$.warningTtlHours") { value(48) }
            jsonPath("$.thresholdAction") { value(PunishmentType.MUTE.name) }
            jsonPath("$.thresholdDurationMinutes") { value(60) }
            jsonPath("$.defaultBlocklistAction") { value(PunishmentType.WARN.name) }
            jsonPath("$.logChannelId") { value(-1001234567890L) }
        }
    }

    @Test
    fun `PUT settings - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"collectionEnabled": false}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `PUT settings - updates collection enabled and logs change`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Initial config with log channel
        moderationConfigRepository.save(
            ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001234567890L
            )
        )

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"collectionEnabled": false}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.collectionEnabled") { value(false) }
        }

        // Verify settings were updated
        val updated = chatSettingsRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(updated?.collectionEnabled).isFalse()
    }

    @Test
    fun `PUT settings - updates moderation config and logs changes`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings(chatTitle = "Test Chat"))

        // Initial config with log channel
        moderationConfigRepository.save(
            ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                cleanServiceEnabled = false,
                maxWarnings = 3,
                logChannelId = -1001234567890L
            )
        )

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "cleanServiceEnabled": true,
                    "maxWarnings": 5
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.cleanServiceEnabled") { value(true) }
            jsonPath("$.maxWarnings") { value(5) }
        }

        // Verify CLEAN_SERVICE_ON log entry was sent (separate action type for cleanService toggle)
        val cleanServiceLog = capturingLogChannelPort.waitForEntry(
            channelId = -1001234567890L,
            actionType = ActionType.CLEAN_SERVICE_ON,
            timeoutMs = 2000
        )

        assertThat(cleanServiceLog).isNotNull
        assertThat(cleanServiceLog?.chatId).isEqualTo(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(cleanServiceLog?.chatTitle).isEqualTo("Test Chat")
        assertThat(cleanServiceLog?.adminId).isEqualTo(user.id)

        // Verify CONFIG_CHANGED log entry for maxWarnings
        val configLog = capturingLogChannelPort.waitForEntry(
            channelId = -1001234567890L,
            actionType = ActionType.CONFIG_CHANGED,
            timeoutMs = 2000
        )

        assertThat(configLog).isNotNull
        assertThat(configLog?.reason).contains("maxWarnings: 5")
    }

    @Test
    fun `PUT settings - validates invalid threshold action`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"thresholdAction": "INVALID_ACTION"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT settings - validates invalid blocklist action`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"defaultBlocklistAction": "INVALID"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT settings - validates max warnings range`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Too low
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"maxWarnings": 0}"""
        }.andExpect {
            status { isBadRequest() }
        }

        // Too high
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"maxWarnings": 25}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT settings - updates multiple settings at once`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        moderationConfigRepository.save(
            ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001234567890L
            )
        )

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "cleanServiceEnabled": true,
                    "maxWarnings": 4,
                    "warningTtlHours": 72,
                    "thresholdAction": "MUTE",
                    "thresholdDurationMinutes": 120,
                    "defaultBlocklistAction": "WARN"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.cleanServiceEnabled") { value(true) }
            jsonPath("$.maxWarnings") { value(4) }
            jsonPath("$.warningTtlHours") { value(72) }
            jsonPath("$.thresholdAction") { value("MUTE") }
            jsonPath("$.thresholdDurationMinutes") { value(120) }
            jsonPath("$.defaultBlocklistAction") { value("WARN") }
        }

        // cleanServiceEnabled is logged with CLEAN_SERVICE_ON action type
        val cleanServiceLog = capturingLogChannelPort.waitForEntry(
            channelId = -1001234567890L,
            actionType = ActionType.CLEAN_SERVICE_ON,
            timeoutMs = 2000
        )
        assertThat(cleanServiceLog).isNotNull

        // Other config changes are logged with CONFIG_CHANGED action type
        val configLog = capturingLogChannelPort.waitForEntry(
            channelId = -1001234567890L,
            actionType = ActionType.CONFIG_CHANGED,
            timeoutMs = 2000
        )

        assertThat(configLog?.reason).contains("maxWarnings: 4")
        assertThat(configLog?.reason).contains("warningTtl: 72h")
        assertThat(configLog?.reason).contains("thresholdAction: MUTE")
        assertThat(configLog?.reason).contains("thresholdDuration: 120min")
        assertThat(configLog?.reason).contains("defaultBlocklistAction: WARN")
    }

    @Test
    fun `PUT settings - does not log when no changes made`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings(collectionEnabled = true))
        moderationConfigRepository.save(
            ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                cleanServiceEnabled = false,
                logChannelId = -1001234567890L
            )
        )

        // Update with same value
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"cleanServiceEnabled": false}"""
        }.andExpect {
            status { isOk() }
        }

        // Verify no log entry sent
        assertThat(capturingLogChannelPort.getEntriesForChannel(-1001234567890L)).isEmpty()
    }

    @Test
    fun `PUT settings - updates log channel ID`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        moderationConfigRepository.save(
            ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001111111111L
            )
        )

        val newLogChannelId = -1002222222222L

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"logChannelId": $newLogChannelId}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.logChannelId") { value(newLogChannelId) }
        }

        // Verify log sent to NEW channel (config is saved before logging)
        val logEntry = capturingLogChannelPort.waitForEntry(
            channelId = newLogChannelId,
            actionType = ActionType.CONFIG_CHANGED,
            timeoutMs = 2000
        )

        assertThat(logEntry).isNotNull
        assertThat(logEntry?.reason).contains("logChannel: $newLogChannelId")
    }
}
