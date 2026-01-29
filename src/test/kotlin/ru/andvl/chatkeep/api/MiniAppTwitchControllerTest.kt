package ru.andvl.chatkeep.api

import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import ru.andvl.chatkeep.api.support.TestDataFactory
import ru.andvl.chatkeep.domain.model.twitch.TwitchChannelSubscription
import ru.andvl.chatkeep.domain.model.twitch.TwitchNotificationSettings
import ru.andvl.chatkeep.domain.model.twitch.TwitchStream
import ru.andvl.chatkeep.domain.service.twitch.TwitchApiClient
import ru.andvl.chatkeep.domain.service.twitch.TwitchSearchChannel
import ru.andvl.chatkeep.domain.service.twitch.TwitchUser
import java.time.Instant

/**
 * Integration tests for MiniAppTwitchController.
 * Tests Twitch channel subscription management.
 */
class MiniAppTwitchControllerTest : MiniAppApiTestBase() {

    @Autowired
    private lateinit var twitchApiClient: TwitchApiClient

    @Test
    fun `GET channels - returns 401 when not authenticated`() {
        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/channels")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `GET channels - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/channels") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET channels - returns empty list when no subscriptions`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/channels") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
            jsonPath("$") { isEmpty() }
        }
    }

    @Test
    fun `GET channels - returns subscribed channels with live status`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Create subscriptions
        val sub1 = twitchChannelSubscriptionRepository.save(
            TwitchChannelSubscription.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                twitchChannelId = "12345",
                twitchLogin = "streamer1",
                displayName = "Streamer One",
                avatarUrl = "https://example.com/avatar1.jpg",
                createdBy = user.id
            )
        )

        val sub2 = twitchChannelSubscriptionRepository.save(
            TwitchChannelSubscription.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                twitchChannelId = "67890",
                twitchLogin = "streamer2",
                displayName = "Streamer Two",
                avatarUrl = "https://example.com/avatar2.jpg",
                createdBy = user.id
            )
        )

        // Create active stream for sub1
        twitchStreamRepository.save(
            TwitchStream.createNew(
                subscriptionId = sub1.id!!,
                twitchStreamId = "stream123",
                startedAt = Instant.now().minusSeconds(3600),
                currentGame = "Just Chatting",
                currentTitle = "Test Stream"
            )
        )

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/channels") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].twitchLogin") { value("streamer1") }
            jsonPath("$[0].displayName") { value("Streamer One") }
            jsonPath("$[0].isLive") { value(true) }
            jsonPath("$[1].twitchLogin") { value("streamer2") }
            jsonPath("$[1].isLive") { value(false) }
        }
    }

    @Test
    fun `POST channels - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/channels") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"twitchLogin": "teststreamer"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST channels - subscribes to channel successfully`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Mock Twitch API response
        every { twitchApiClient.getUserByLogin("teststreamer") } returns TwitchUser(
            id = "98765",
            login = "teststreamer",
            displayName = "Test Streamer",
            profileImageUrl = "https://example.com/avatar.jpg",
            description = null
        )

        every {
            twitchApiClient.createEventSubSubscription(
                type = "stream.online",
                version = "1",
                condition = mapOf("broadcaster_user_id" to "98765")
            )
        } returns io.mockk.mockk {
            every { id } returns "eventsub123"
        }

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/channels") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"twitchLogin": "teststreamer"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.twitchLogin") { value("teststreamer") }
            jsonPath("$.displayName") { value("Test Streamer") }
            jsonPath("$.twitchChannelId") { value("98765") }
            jsonPath("$.isLive") { value(false) }
        }

        // Verify subscription was saved
        val subscriptions = twitchChannelSubscriptionRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(subscriptions).hasSize(1)
        assertThat(subscriptions[0].twitchLogin).isEqualTo("teststreamer")
    }

    @Test
    fun `POST channels - returns error when limit reached (5 channels)`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Create 5 existing subscriptions (limit is 5)
        repeat(5) { index ->
            twitchChannelSubscriptionRepository.save(
                TwitchChannelSubscription.createNew(
                    chatId = TestDataFactory.DEFAULT_CHAT_ID,
                    twitchChannelId = "id$index",
                    twitchLogin = "streamer$index",
                    displayName = "Streamer $index",
                    avatarUrl = null,
                    createdBy = user.id
                )
            )
        }

        every { twitchApiClient.getUserByLogin("newstreamer") } returns TwitchUser(
            id = "99999",
            login = "newstreamer",
            displayName = "New Streamer",
            profileImageUrl = null,
            description = null
        )

        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/channels") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"twitchLogin": "newstreamer"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST channels - returns 400 if channel already subscribed`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Create existing subscription
        twitchChannelSubscriptionRepository.save(
            TwitchChannelSubscription.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                twitchChannelId = "12345",
                twitchLogin = "existingstreamer",
                displayName = "Existing Streamer",
                avatarUrl = null,
                createdBy = user.id
            )
        )

        every { twitchApiClient.getUserByLogin("existingstreamer") } returns TwitchUser(
            id = "12345",
            login = "existingstreamer",
            displayName = "Existing Streamer",
            profileImageUrl = null,
            description = null
        )

        // Service returns null for duplicates, controller throws 400 Bad Request
        mockMvc.post("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/channels") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"twitchLogin": "existingstreamer"}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isBadRequest() }
        }

        // Verify no new subscription was created
        val subscriptions = twitchChannelSubscriptionRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(subscriptions).hasSize(1)
    }

    @Test
    fun `DELETE channels - unsubscribes from channel`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        val subscription = twitchChannelSubscriptionRepository.save(
            TwitchChannelSubscription.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                twitchChannelId = "12345",
                twitchLogin = "teststreamer",
                displayName = "Test Streamer",
                avatarUrl = null,
                createdBy = user.id
            ).copy(eventsubSubscriptionId = "eventsub123")
        )

        every { twitchApiClient.deleteEventSubSubscription("eventsub123") } returns Unit

        mockMvc.delete("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/channels/${subscription.id}") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isNoContent() }
        }

        // Verify subscription was deleted
        val subscriptions = twitchChannelSubscriptionRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(subscriptions).isEmpty()

        verify { twitchApiClient.deleteEventSubSubscription("eventsub123") }
    }

    @Test
    fun `GET search - searches channels and rate limits`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        every { twitchApiClient.searchChannels("test") } returns listOf(
            TwitchSearchChannel(
                id = "123",
                broadcaster_login = "testuser",
                display_name = "Test User",
                thumbnail_url = "https://example.com/thumb.jpg",
                is_live = true
            ),
            TwitchSearchChannel(
                id = "456",
                broadcaster_login = "testgamer",
                display_name = "Test Gamer",
                thumbnail_url = "https://example.com/thumb2.jpg",
                is_live = false
            )
        )

        mockMvc.get("/api/v1/miniapp/twitch/search?query=test") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].login") { value("testuser") }
            jsonPath("$[0].displayName") { value("Test User") }
            jsonPath("$[0].isLive") { value(true) }
            jsonPath("$[1].login") { value("testgamer") }
            jsonPath("$[1].isLive") { value(false) }
        }
    }

    @Test
    fun `GET settings - returns notification settings`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Create custom settings
        twitchNotificationSettingsRepository.save(
            TwitchNotificationSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                messageTemplate = "Custom template: {streamer} is live!"
            )
        )

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/settings") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.messageTemplate") { value("Custom template: {streamer} is live!") }
        }
    }

    @Test
    fun `PUT settings - updates message template`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/twitch/settings") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "messageTemplate": "Updated: {streamer} started streaming!",
                "endedMessageTemplate": "Stream ended: {streamer}",
                "buttonText": "📺 Watch Now"
            }"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.messageTemplate") { value("Updated: {streamer} started streaming!") }
            jsonPath("$.endedMessageTemplate") { value("Stream ended: {streamer}") }
            jsonPath("$.buttonText") { value("📺 Watch Now") }
        }

        // Verify settings were saved
        val settings = twitchNotificationSettingsRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(settings).isNotNull
        assertThat(settings?.messageTemplate).isEqualTo("Updated: {streamer} started streaming!")
        assertThat(settings?.endedMessageTemplate).isEqualTo("Stream ended: {streamer}")
        assertThat(settings?.buttonText).isEqualTo("📺 Watch Now")
    }
}
