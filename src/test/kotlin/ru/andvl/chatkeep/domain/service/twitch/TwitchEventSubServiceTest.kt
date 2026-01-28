package ru.andvl.chatkeep.domain.service.twitch

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.twitch.StreamTimelineEvent
import ru.andvl.chatkeep.domain.model.twitch.TwitchChannelSubscription
import ru.andvl.chatkeep.domain.model.twitch.TwitchStream
import ru.andvl.chatkeep.infrastructure.repository.twitch.StreamTimelineEventRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchChannelSubscriptionRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchStreamRepository
import java.time.Instant
import java.util.*

/**
 * Unit tests for TwitchEventSubService.
 * Tests webhook event handling for stream.online and stream.offline events.
 */
class TwitchEventSubServiceTest {

    private val channelRepo: TwitchChannelSubscriptionRepository = mockk()
    private val streamRepo: TwitchStreamRepository = mockk()
    private val timelineRepo: StreamTimelineEventRepository = mockk()
    private val notificationService: TwitchNotificationService = mockk()
    private val twitchApiClient: TwitchApiClient = mockk()

    private lateinit var eventSubService: TwitchEventSubService

    @BeforeEach
    fun setup() {
        clearAllMocks()
        eventSubService = TwitchEventSubService(
            channelRepo = channelRepo,
            streamRepo = streamRepo,
            timelineRepo = timelineRepo,
            notificationService = notificationService,
            twitchApiClient = twitchApiClient
        )
    }

    @Test
    fun `handleStreamOnline - creates stream record and sends notification`() = runBlocking {
        // Setup subscription
        val subscription = createSubscription(id = 1L, twitchChannelId = "12345")

        every { channelRepo.findByTwitchChannelId("12345") } returns listOf(subscription)
        every { streamRepo.findActiveBySubscriptionId(1L) } returns null
        every { streamRepo.save(any()) } answers {
            firstArg<TwitchStream>().copy(id = 100L)
        }
        every { timelineRepo.save(any()) } answers { firstArg() }
        every { twitchApiClient.getStreams(listOf("12345")) } returns listOf(
            TwitchStreamData(
                id = "stream123",
                userId = "12345",
                userLogin = "teststreamer",
                userName = "Test Streamer",
                gameId = "509658",
                gameName = "Just Chatting",
                title = "Test Stream Title",
                viewerCount = 100,
                startedAt = "2024-01-01T12:00:00Z",
                thumbnailUrl = "https://example.com/thumb.jpg"
            )
        )
        coEvery { notificationService.sendStreamStartNotification(any(), any(), any(), any(), any()) } returns 12345L

        // Handle stream online event
        eventSubService.handleStreamOnline(
            broadcasterId = "12345",
            streamId = "stream123",
            startedAt = "2024-01-01T12:00:00Z"
        )

        // Wait for async notification
        Thread.sleep(200)

        // Verify stream record was saved
        verify { streamRepo.save(match {
            it.twitchStreamId == "stream123" &&
            it.currentGame == "Just Chatting" &&
            it.currentTitle == "Test Stream Title"
        }) }

        // Verify timeline event was created
        verify { timelineRepo.save(match {
            it.streamOffsetSeconds == 0 && it.gameName == "Just Chatting"
        }) }

        // Verify notification was sent
        coVerify { notificationService.sendStreamStartNotification(
            chatId = -1001234567890L,
            stream = any(),
            streamerName = "Test Streamer",
            streamerLogin = "teststreamer",
            thumbnailUrl = "https://example.com/thumb.jpg"
        ) }
    }

    @Test
    fun `handleStreamOnline - does not create duplicate stream`() {
        val subscription = createSubscription(id = 1L, twitchChannelId = "12345")
        val existingStream = createStream(id = 100L, subscriptionId = 1L)

        every { channelRepo.findByTwitchChannelId("12345") } returns listOf(subscription)
        every { streamRepo.findActiveBySubscriptionId(1L) } returns existingStream
        every { twitchApiClient.getStreams(any()) } returns emptyList()

        eventSubService.handleStreamOnline(
            broadcasterId = "12345",
            streamId = "stream123",
            startedAt = "2024-01-01T12:00:00Z"
        )

        // Verify no new stream was saved
        verify(exactly = 0) { streamRepo.save(any()) }
    }

    @Test
    fun `handleStreamOnline - handles multiple subscriptions for same channel`() = runBlocking {
        val sub1 = createSubscription(id = 1L, chatId = -1001111111111L, twitchChannelId = "12345")
        val sub2 = createSubscription(id = 2L, chatId = -1002222222222L, twitchChannelId = "12345")

        every { channelRepo.findByTwitchChannelId("12345") } returns listOf(sub1, sub2)
        every { streamRepo.findActiveBySubscriptionId(any()) } returns null
        every { streamRepo.save(any()) } answers {
            firstArg<TwitchStream>().copy(id = (100L + Math.random() * 100).toLong())
        }
        every { timelineRepo.save(any()) } answers { firstArg() }
        every { twitchApiClient.getStreams(any()) } returns listOf(
            TwitchStreamData(
                id = "stream123",
                userId = "12345",
                userLogin = "teststreamer",
                userName = "Test Streamer",
                gameId = "509658",
                gameName = "Just Chatting",
                title = "Test Stream",
                viewerCount = 50,
                startedAt = "2024-01-01T12:00:00Z",
                thumbnailUrl = "https://example.com/thumb.jpg"
            )
        )
        coEvery { notificationService.sendStreamStartNotification(any(), any(), any(), any(), any()) } returns 12345L

        eventSubService.handleStreamOnline(
            broadcasterId = "12345",
            streamId = "stream123",
            startedAt = "2024-01-01T12:00:00Z"
        )

        Thread.sleep(200)

        // Verify stream records were saved for both subscriptions
        // Initial saves + async updates with telegram message IDs = 4 total calls
        verify(atLeast = 2) { streamRepo.save(any()) }
        // Verify both subscriptions had streams saved
        verify { streamRepo.save(match { it.subscriptionId == 1L }) }
        verify { streamRepo.save(match { it.subscriptionId == 2L }) }
    }

    @Test
    fun `handleStreamOffline - updates stream status to ended`() {
        val subscription = createSubscription(id = 1L, twitchChannelId = "12345")
        val stream = createStream(id = 100L, subscriptionId = 1L)

        every { channelRepo.findByTwitchChannelId("12345") } returns listOf(subscription)
        every { streamRepo.findActiveBySubscriptionId(1L) } returns stream
        every { streamRepo.save(any()) } answers { firstArg() }

        eventSubService.handleStreamOffline(broadcasterId = "12345")

        // Verify stream was updated with ended status
        verify { streamRepo.save(match {
            it.status == "ended" && it.endedAt != null
        }) }
    }

    @Test
    fun `handleStreamOffline - edits Telegram message with timeline`() = runBlocking {
        val subscription = createSubscription(id = 1L, twitchChannelId = "12345")
        val stream = createStream(
            id = 100L,
            subscriptionId = 1L,
            telegramMessageId = 999L,
            telegramChatId = -1001234567890L
        )
        val timelineEvents = listOf(
            StreamTimelineEvent.createNew(
                streamId = 100L,
                streamOffsetSeconds = 0,
                gameName = "Just Chatting",
                streamTitle = "Test Stream"
            )
        )

        every { channelRepo.findByTwitchChannelId("12345") } returns listOf(subscription)
        every { streamRepo.findActiveBySubscriptionId(1L) } returns stream
        every { streamRepo.save(any()) } answers { firstArg() }
        every { timelineRepo.findByStreamId(100L) } returns timelineEvents
        coEvery { notificationService.updateStreamNotification(any(), any(), any(), any(), any(), any()) } returns null

        eventSubService.handleStreamOffline(broadcasterId = "12345")

        Thread.sleep(200)

        // Verify notification was edited
        coVerify { notificationService.updateStreamNotification(
            chatId = -1001234567890L,
            messageId = 999L,
            stream = any(),
            streamerName = "Test Streamer",
            streamerLogin = "teststreamer",
            timeline = timelineEvents
        ) }
    }

    @Test
    fun `handleStreamOffline - handles missing stream gracefully`() {
        val subscription = createSubscription(id = 1L, twitchChannelId = "12345")

        every { channelRepo.findByTwitchChannelId("12345") } returns listOf(subscription)
        every { streamRepo.findActiveBySubscriptionId(1L) } returns null

        // Should not throw
        eventSubService.handleStreamOffline(broadcasterId = "12345")

        // Verify no save was attempted
        verify(exactly = 0) { streamRepo.save(any()) }
    }

    @Test
    fun `handleStreamOnline - does nothing when no subscriptions found`() {
        every { channelRepo.findByTwitchChannelId("99999") } returns emptyList()

        eventSubService.handleStreamOnline(
            broadcasterId = "99999",
            streamId = "stream999",
            startedAt = "2024-01-01T12:00:00Z"
        )

        // Verify no API calls or saves
        verify(exactly = 0) { twitchApiClient.getStreams(any()) }
        verify(exactly = 0) { streamRepo.save(any()) }
    }

    @Test
    fun `handleStreamOffline - does nothing when no subscriptions found`() {
        every { channelRepo.findByTwitchChannelId("99999") } returns emptyList()

        eventSubService.handleStreamOffline(broadcasterId = "99999")

        verify(exactly = 0) { streamRepo.save(any()) }
    }

    // Helper methods

    private fun createSubscription(
        id: Long = 1L,
        chatId: Long = -1001234567890L,
        twitchChannelId: String = "12345"
    ) = TwitchChannelSubscription(
        id = id,
        chatId = chatId,
        twitchChannelId = twitchChannelId,
        twitchLogin = "teststreamer",
        displayName = "Test Streamer",
        avatarUrl = null,
        createdBy = 123456L,
        createdAt = Instant.now(),
        eventsubSubscriptionId = null
    )

    private fun createStream(
        id: Long = 100L,
        subscriptionId: Long = 1L,
        telegramMessageId: Long? = null,
        telegramChatId: Long? = null
    ) = TwitchStream(
        id = id,
        subscriptionId = subscriptionId,
        twitchStreamId = "stream123",
        startedAt = Instant.now().minusSeconds(3600),
        endedAt = null,
        status = "live",
        currentGame = "Just Chatting",
        currentTitle = "Test Stream",
        viewerCount = 100,
        telegramMessageId = telegramMessageId,
        telegramChatId = telegramChatId
    )
}
