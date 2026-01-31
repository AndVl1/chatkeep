package ru.andvl.chatkeep.domain.service.twitch

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.edit.caption.EditChatMessageCaption
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.twitch.StreamTimelineEvent
import ru.andvl.chatkeep.domain.model.twitch.TwitchNotificationSettings
import ru.andvl.chatkeep.domain.model.twitch.TwitchStream
import ru.andvl.chatkeep.infrastructure.repository.twitch.StreamTimelineEventRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchNotificationSettingsRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TwitchNotificationServiceTest {

    private val mockBot = mockk<TelegramBot>()
    private val mockSettingsRepo = mockk<TwitchNotificationSettingsRepository>()
    private val mockTimelineRepo = mockk<StreamTimelineEventRepository>()
    private val mockTelegraphService = mockk<TelegraphService>()

    private val service = TwitchNotificationService(
        bot = mockBot,
        settingsRepository = mockSettingsRepo,
        timelineRepository = mockTimelineRepo,
        telegraphService = mockTelegraphService
    )

    @Test
    fun `updateStreamNotification should create Telegraph button when timeline exceeds 1000 chars`() = runTest {
        // Arrange
        val chatId = 123456L
        val messageId = 789L
        val streamerName = "TestStreamer"
        val streamerLogin = "teststreamer"
        val telegraphUrl = "https://telegra.ph/test-page"

        val settings = TwitchNotificationSettings.createNew(chatId)

        // Create stream
        val stream = TwitchStream.createNew(
            subscriptionId = 1L,
            twitchStreamId = "stream123",
            startedAt = Instant.now().minusSeconds(3600),
            currentGame = "Test Game",
            currentTitle = "Test Stream Title",
            hasPhoto = true
        ).copy(
            id = 1L,
            viewerCount = 1000
        )

        // Create a long timeline (30 events to exceed 1000 chars when formatted)
        val timeline = (0..29).map { index ->
            StreamTimelineEvent.createNew(
                streamId = 1L,
                streamOffsetSeconds = index * 600, // Every 10 minutes
                gameName = "Game $index with a reasonably long name to increase character count",
                streamTitle = "Stream title $index with detailed description to make it longer"
            ).copy(id = index.toLong())
        }

        // Mock settings repository
        every { mockSettingsRepo.findByChatId(chatId) } returns settings

        // Mock Telegraph service (should be called because timeline is long)
        coEvery {
            mockTelegraphService.createOrUpdateTimelinePage(
                streamerId = streamerLogin,
                streamerName = streamerName,
                timeline = timeline
            )
        } returns telegraphUrl

        // Mock bot response (use relaxed mock to avoid type mismatch)
        val requestSlot = slot<EditChatMessageCaption>()
        coEvery { mockBot.execute(capture(requestSlot)) } returns mockk(relaxed = true)

        // Act
        val result = service.updateStreamNotification(
            chatId = chatId,
            messageId = messageId,
            stream = stream,
            streamerName = streamerName,
            streamerLogin = streamerLogin,
            timeline = timeline,
            thumbnailUrl = null
        )

        // Assert
        // Verify Telegraph URL was returned
        assertEquals(telegraphUrl, result)

        // Verify Telegraph service was called
        coVerify {
            mockTelegraphService.createOrUpdateTimelinePage(
                streamerId = streamerLogin,
                streamerName = streamerName,
                timeline = timeline
            )
        }

        // Verify bot.execute was called
        coVerify { mockBot.execute(any<EditChatMessageCaption>()) }

        // Verify the keyboard has 2 rows: stream button + Telegraph button
        val capturedRequest = requestSlot.captured
        val keyboard = capturedRequest.replyMarkup
        assertNotNull(keyboard)
        assertEquals(2, keyboard.keyboard.size, "Keyboard should have 2 rows: stream button and Telegraph button")

        // Verify first row has 1 button (stream button)
        val firstRow = keyboard.keyboard[0]
        assertEquals(1, firstRow.size, "First row should have 1 button (stream link)")

        // Verify second row has 1 button (Telegraph button)
        val secondRow = keyboard.keyboard[1]
        assertEquals(1, secondRow.size, "Second row should have 1 button (Telegraph link)")
    }

    @Test
    fun `updateStreamNotification should NOT create Telegraph button when timeline fits in 1000 chars`() = runTest {
        // Arrange
        val chatId = 123456L
        val messageId = 789L
        val streamerName = "TestStreamer"
        val streamerLogin = "teststreamer"

        val settings = TwitchNotificationSettings.createNew(chatId)

        // Create stream
        val stream = TwitchStream.createNew(
            subscriptionId = 1L,
            twitchStreamId = "stream123",
            startedAt = Instant.now().minusSeconds(3600),
            currentGame = "Game",
            currentTitle = "Title",
            hasPhoto = true
        ).copy(
            id = 1L,
            viewerCount = 100
        )

        // Create a SHORT timeline (only 2 events - will NOT exceed 1000 chars)
        val timeline = listOf(
            StreamTimelineEvent.createNew(
                streamId = 1L,
                streamOffsetSeconds = 0,
                gameName = "Game 1",
                streamTitle = "Title 1"
            ).copy(id = 1L),
            StreamTimelineEvent.createNew(
                streamId = 1L,
                streamOffsetSeconds = 600,
                gameName = "Game 2",
                streamTitle = "Title 2"
            ).copy(id = 2L)
        )

        // Mock settings repository
        every { mockSettingsRepo.findByChatId(chatId) } returns settings

        // Mock bot response (use relaxed mock to avoid type mismatch)
        val requestSlot = slot<EditChatMessageCaption>()
        coEvery { mockBot.execute(capture(requestSlot)) } returns mockk(relaxed = true)

        // Act
        val result = service.updateStreamNotification(
            chatId = chatId,
            messageId = messageId,
            stream = stream,
            streamerName = streamerName,
            streamerLogin = streamerLogin,
            timeline = timeline,
            thumbnailUrl = null
        )

        // Assert
        // Verify NO Telegraph URL was returned
        assertEquals(null, result)

        // Verify Telegraph service was NOT called
        coVerify(exactly = 0) {
            mockTelegraphService.createOrUpdateTimelinePage(any(), any(), any())
        }

        // Verify bot.execute was called
        coVerify { mockBot.execute(any<EditChatMessageCaption>()) }

        // Verify the keyboard has ONLY 1 row: stream button (no Telegraph button)
        val capturedRequest = requestSlot.captured
        val keyboard = capturedRequest.replyMarkup
        assertNotNull(keyboard)
        assertEquals(1, keyboard.keyboard.size, "Keyboard should have only 1 row: stream button")

        // Verify the row has 1 button (stream button)
        val firstRow = keyboard.keyboard[0]
        assertEquals(1, firstRow.size, "First row should have 1 button (stream link)")
    }
}
