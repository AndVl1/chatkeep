package ru.andvl.chatkeep.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import ru.andvl.chatkeep.api.support.TestDataFactory
import ru.andvl.chatkeep.domain.model.gated.ChatGatedFeature
import java.time.Instant

/**
 * Integration tests for MiniAppGatedFeaturesController.
 * Tests feature gating functionality.
 */
class MiniAppGatedFeaturesControllerTest : MiniAppApiTestBase() {

    @Test
    fun `GET features - returns 401 when not authenticated`() {
        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `GET features - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET features - returns all features with default disabled status`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
            jsonPath("$.length()") { value(1) } // Only TWITCH_INTEGRATION exists
            jsonPath("$[0].key") { value("twitch_integration") }
            jsonPath("$[0].enabled") { value(false) }
            jsonPath("$[0].name") { value("Twitch уведомления") }
            jsonPath("$[0].description") { exists() }
            jsonPath("$[0].enabledAt") { doesNotExist() }
            jsonPath("$[0].enabledBy") { doesNotExist() }
        }
    }

    @Test
    fun `GET features - returns enabled status when feature is enabled`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Enable the feature
        val enabledAt = Instant.now()
        chatGatedFeatureRepository.save(
            ChatGatedFeature.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                featureKey = "twitch_integration",
                enabled = true,
                enabledBy = user.id
            ).copy(enabledAt = enabledAt)
        )

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features") {
            header("Authorization", authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].key") { value("twitch_integration") }
            jsonPath("$[0].enabled") { value(true) }
            jsonPath("$[0].enabledAt") { exists() }
            jsonPath("$[0].enabledBy") { value(user.id) }
        }
    }

    @Test
    fun `PUT feature - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features/twitch_integration") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": true}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `PUT feature - enables feature successfully`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features/twitch_integration") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": true}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.key") { value("twitch_integration") }
            jsonPath("$.enabled") { value(true) }
            jsonPath("$.name") { value("Twitch уведомления") }
            jsonPath("$.enabledAt") { exists() }
            jsonPath("$.enabledBy") { value(user.id) }
        }

        // Verify feature was saved
        val feature = chatGatedFeatureRepository.findByChatIdAndFeatureKey(
            TestDataFactory.DEFAULT_CHAT_ID,
            "twitch_integration"
        )
        assertThat(feature).isNotNull
        assertThat(feature?.enabled).isTrue()
        assertThat(feature?.enabledBy).isEqualTo(user.id)
    }

    @Test
    fun `PUT feature - disables feature successfully`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // First enable the feature
        chatGatedFeatureRepository.save(
            ChatGatedFeature.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                featureKey = "twitch_integration",
                enabled = true,
                enabledBy = user.id
            )
        )

        // Now disable it
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features/twitch_integration") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": false}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.key") { value("twitch_integration") }
            jsonPath("$.enabled") { value(false) }
        }

        // Verify feature was updated
        val feature = chatGatedFeatureRepository.findByChatIdAndFeatureKey(
            TestDataFactory.DEFAULT_CHAT_ID,
            "twitch_integration"
        )
        assertThat(feature).isNotNull
        assertThat(feature?.enabled).isFalse()
    }

    @Test
    fun `PUT feature - returns 400 for unknown feature key`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Unknown feature key causes IllegalArgumentException which returns 400 Bad Request
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features/unknown_feature") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": true}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT feature - sets enabledAt and enabledBy when enabling`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        val beforeEnable = Instant.now()

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features/twitch_integration") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": true}"""
        }.andExpect {
            status { isOk() }
        }

        val afterEnable = Instant.now()

        val feature = chatGatedFeatureRepository.findByChatIdAndFeatureKey(
            TestDataFactory.DEFAULT_CHAT_ID,
            "twitch_integration"
        )
        assertThat(feature).isNotNull
        assertThat(feature?.enabledAt).isNotNull
        assertThat(feature?.enabledAt).isBetween(beforeEnable, afterEnable)
        assertThat(feature?.enabledBy).isEqualTo(user.id)
    }

    @Test
    fun `PUT feature - re-enabling updates enabledAt and enabledBy`() {
        val user1 = testDataFactory.createTelegramUser(id = 111L)
        val user2 = testDataFactory.createTelegramUser(id = 222L)
        val authHeader1 = authTestHelper.createValidAuthHeader(user1)
        val authHeader2 = authTestHelper.createValidAuthHeader(user2)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Enable by user1
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user1.id)
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features/twitch_integration") {
            header("Authorization", authHeader1)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": true}"""
        }.andExpect {
            status { isOk() }
        }

        val firstEnable = chatGatedFeatureRepository.findByChatIdAndFeatureKey(
            TestDataFactory.DEFAULT_CHAT_ID,
            "twitch_integration"
        )

        Thread.sleep(10) // Small delay to ensure different timestamp

        // Disable by user2
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user2.id)
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features/twitch_integration") {
            header("Authorization", authHeader2)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": false}"""
        }.andExpect {
            status { isOk() }
        }

        // Re-enable by user2
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features/twitch_integration") {
            header("Authorization", authHeader2)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": true}"""
        }.andExpect {
            status { isOk() }
        }

        val reEnabled = chatGatedFeatureRepository.findByChatIdAndFeatureKey(
            TestDataFactory.DEFAULT_CHAT_ID,
            "twitch_integration"
        )

        // enabledAt should be updated, enabledBy should be user2
        assertThat(reEnabled?.enabledAt).isAfter(firstEnable?.enabledAt)
        assertThat(reEnabled?.enabledBy).isEqualTo(user2.id)
    }

    @Test
    fun `PUT feature - preserves enabledAt when disabling`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Enable the feature
        val originalEnabledAt = Instant.now().minusSeconds(3600)
        chatGatedFeatureRepository.save(
            ChatGatedFeature.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                featureKey = "twitch_integration",
                enabled = true,
                enabledBy = user.id
            ).copy(enabledAt = originalEnabledAt)
        )

        // Disable it
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/features/twitch_integration") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled": false}"""
        }.andExpect {
            status { isOk() }
        }

        // enabledAt should be preserved when disabling (use truncated comparison for DB precision)
        val feature = chatGatedFeatureRepository.findByChatIdAndFeatureKey(
            TestDataFactory.DEFAULT_CHAT_ID,
            "twitch_integration"
        )
        // Compare truncated to seconds to avoid nanosecond precision issues with PostgreSQL
        assertThat(feature?.enabledAt?.epochSecond).isEqualTo(originalEnabledAt.epochSecond)
        assertThat(feature?.enabledBy).isEqualTo(user.id)
    }
}
