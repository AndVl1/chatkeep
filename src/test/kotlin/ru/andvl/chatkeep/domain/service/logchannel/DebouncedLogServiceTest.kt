package ru.andvl.chatkeep.domain.service.logchannel

import io.mockk.mockk
import io.mockk.verify
import io.mockk.confirmVerified
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry

class DebouncedLogServiceTest {

    private lateinit var logChannelService: LogChannelService
    private lateinit var debouncedLogService: DebouncedLogService

    @BeforeEach
    fun setup() {
        logChannelService = mockk(relaxed = true)
        // Use a short debounce delay for testing (500ms)
        debouncedLogService = DebouncedLogService(logChannelService, 500)
    }

    @Test
    fun `non-debounced actions are sent immediately`() {
        // Given a non-debounced action (like BAN)
        val entry = createEntry(ActionType.BAN, chatId = 1L)

        // When logging the action
        debouncedLogService.logAction(entry)

        // Then it should be sent immediately
        verify(exactly = 1) { logChannelService.logModerationAction(entry) }
    }

    @Test
    fun `debounced actions are delayed`() = runBlocking {
        // Given a debounced action (WELCOME_CHANGED)
        val entry = createEntry(ActionType.WELCOME_CHANGED, chatId = 1L)

        // When logging the action
        debouncedLogService.logAction(entry)

        // Then it should NOT be sent immediately
        verify(exactly = 0) { logChannelService.logModerationAction(any()) }

        // Wait for debounce delay
        delay(600)

        // Then it should be sent after delay
        verify(exactly = 1) { logChannelService.logModerationAction(entry) }
    }

    @Test
    fun `rapid debounced actions are coalesced`() = runBlocking {
        // Given multiple rapid updates to the same action
        val entry1 = createEntry(ActionType.RULES_CHANGED, chatId = 1L, reason = "update 1")
        val entry2 = createEntry(ActionType.RULES_CHANGED, chatId = 1L, reason = "update 2")
        val entry3 = createEntry(ActionType.RULES_CHANGED, chatId = 1L, reason = "update 3")

        // When logging rapidly
        debouncedLogService.logAction(entry1)
        delay(100)
        debouncedLogService.logAction(entry2)
        delay(100)
        debouncedLogService.logAction(entry3)

        // Then nothing should be sent yet
        verify(exactly = 0) { logChannelService.logModerationAction(any()) }

        // Wait for debounce delay
        delay(600)

        // Then only the last entry should be sent
        verify(exactly = 1) { logChannelService.logModerationAction(entry3) }
        verify(exactly = 0) { logChannelService.logModerationAction(entry1) }
        verify(exactly = 0) { logChannelService.logModerationAction(entry2) }
    }

    @Test
    fun `different chats are debounced independently`() = runBlocking {
        // Given entries for different chats
        val entry1 = createEntry(ActionType.WELCOME_CHANGED, chatId = 1L)
        val entry2 = createEntry(ActionType.WELCOME_CHANGED, chatId = 2L)

        // When logging for different chats
        debouncedLogService.logAction(entry1)
        debouncedLogService.logAction(entry2)

        // Wait for debounce delay
        delay(600)

        // Then both should be sent
        verify(exactly = 1) { logChannelService.logModerationAction(entry1) }
        verify(exactly = 1) { logChannelService.logModerationAction(entry2) }
    }

    @Test
    fun `different action types are debounced independently`() = runBlocking {
        // Given different action types for the same chat
        val welcomeEntry = createEntry(ActionType.WELCOME_CHANGED, chatId = 1L)
        val rulesEntry = createEntry(ActionType.RULES_CHANGED, chatId = 1L)

        // When logging different types
        debouncedLogService.logAction(welcomeEntry)
        debouncedLogService.logAction(rulesEntry)

        // Wait for debounce delay
        delay(600)

        // Then both should be sent
        verify(exactly = 1) { logChannelService.logModerationAction(welcomeEntry) }
        verify(exactly = 1) { logChannelService.logModerationAction(rulesEntry) }
    }

    @Test
    fun `flushAll sends all pending entries immediately`() {
        // Given pending entries
        val entry1 = createEntry(ActionType.WELCOME_CHANGED, chatId = 1L)
        val entry2 = createEntry(ActionType.RULES_CHANGED, chatId = 2L)

        debouncedLogService.logAction(entry1)
        debouncedLogService.logAction(entry2)

        // When flushing all
        debouncedLogService.flushAll()

        // Then all should be sent immediately
        verify(exactly = 1) { logChannelService.logModerationAction(entry1) }
        verify(exactly = 1) { logChannelService.logModerationAction(entry2) }
    }

    @Test
    fun `flushForChat sends only that chat's entries`() {
        // Given entries for different chats
        val entry1 = createEntry(ActionType.WELCOME_CHANGED, chatId = 1L)
        val entry2 = createEntry(ActionType.WELCOME_CHANGED, chatId = 2L)

        debouncedLogService.logAction(entry1)
        debouncedLogService.logAction(entry2)

        // When flushing only chat 1
        debouncedLogService.flushForChat(1L)

        // Then only chat 1's entry should be sent
        verify(exactly = 1) { logChannelService.logModerationAction(entry1) }
        verify(exactly = 0) { logChannelService.logModerationAction(entry2) }
    }

    @Test
    fun `cancelAll removes pending entries without sending`() = runBlocking {
        // Given a pending entry
        val entry = createEntry(ActionType.WELCOME_CHANGED, chatId = 1L)
        debouncedLogService.logAction(entry)

        // When cancelling all
        debouncedLogService.cancelAll()

        // Wait longer than debounce delay
        delay(600)

        // Then nothing should be sent
        verify(exactly = 0) { logChannelService.logModerationAction(any()) }
    }

    @Test
    fun `twitch channel add is NOT debounced`() {
        // Given a Twitch channel add action
        val entry = createEntry(ActionType.TWITCH_CHANNEL_ADDED, chatId = 1L)

        // When logging
        debouncedLogService.logAction(entry)

        // Then it should be sent immediately
        verify(exactly = 1) { logChannelService.logModerationAction(entry) }
    }

    @Test
    fun `twitch settings change IS debounced`() = runBlocking {
        // Given a Twitch settings change action
        val entry = createEntry(ActionType.TWITCH_SETTINGS_CHANGED, chatId = 1L)

        // When logging
        debouncedLogService.logAction(entry)

        // Then it should NOT be sent immediately
        verify(exactly = 0) { logChannelService.logModerationAction(any()) }

        // Wait for debounce delay
        delay(600)

        // Then it should be sent after delay
        verify(exactly = 1) { logChannelService.logModerationAction(entry) }
    }

    private fun createEntry(
        actionType: ActionType,
        chatId: Long,
        reason: String? = null
    ) = ModerationLogEntry(
        chatId = chatId,
        chatTitle = "Test Chat",
        adminId = 123L,
        adminFirstName = "Test",
        adminLastName = "Admin",
        adminUserName = "testadmin",
        actionType = actionType,
        reason = reason,
        source = PunishmentSource.MANUAL
    )
}
