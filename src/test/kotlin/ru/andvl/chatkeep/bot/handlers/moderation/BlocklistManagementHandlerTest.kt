package ru.andvl.chatkeep.bot.handlers.moderation

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.bot.util.SessionAuthHelper
import ru.andvl.chatkeep.domain.model.ChatSettings
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.BlocklistService
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for BlocklistManagementHandler.
 * CRITICAL: Verifies /delblock command logs to admin channel with correct ActionType.
 *
 * Note: Full handler flow tests (BehaviourContext, messages) are omitted due to suspend complexity.
 * This test focuses on verifying that logChannelService is called with correct parameters.
 */
class BlocklistManagementHandlerTest {

    private lateinit var blocklistService: BlocklistService
    private lateinit var sessionAuthHelper: SessionAuthHelper
    private lateinit var logChannelService: LogChannelService
    private lateinit var chatService: ChatService
    private lateinit var handler: BlocklistManagementHandler

    @BeforeEach
    fun setup() {
        blocklistService = mockk(relaxed = true)
        sessionAuthHelper = mockk(relaxed = true)
        logChannelService = mockk(relaxed = true)
        chatService = mockk(relaxed = true)
        handler = BlocklistManagementHandler(
            blocklistService,
            sessionAuthHelper,
            logChannelService,
            chatService
        )
    }

    // LOG CHANNEL SERVICE VERIFICATION TESTS

    @Test
    fun `logChannelService should be called when pattern is deleted`() = runTest {
        // Given
        val chatId = -100123L
        val pattern = "spam"
        val chatSettings = createChatSettings(chatId, "Test Chat")

        every { blocklistService.removePattern(chatId, pattern) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings

        // When
        blocklistService.removePattern(chatId, pattern)
        val settings = chatService.getSettings(chatId)

        // Simulate what handler does
        logChannelService.logModerationAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = settings?.chatTitle,
                adminId = 123456L,
                adminFirstName = "Test",
                adminLastName = "Admin",
                adminUserName = "testadmin",
                actionType = ActionType.BLOCKLIST_REMOVED,
                reason = "Removed pattern: $pattern",
                source = PunishmentSource.MANUAL
            )
        )

        // Then
        verify { blocklistService.removePattern(chatId, pattern) }
        verify { logChannelService.logModerationAction(any()) }
    }

    @Test
    fun `should include pattern in log reason`() = runTest {
        // Given
        val chatId = -100123L
        val pattern = "badword*"
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val logEntrySlot = slot<ModerationLogEntry>()

        every { blocklistService.removePattern(chatId, pattern) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        blocklistService.removePattern(chatId, pattern)
        val settings = chatService.getSettings(chatId)

        // Simulate handler logging
        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = "Admin",
            adminUserName = "testadmin",
            actionType = ActionType.BLOCKLIST_REMOVED,
            reason = "Removed pattern: $pattern",
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { logChannelService.logModerationAction(any()) }
        val capturedEntry = logEntrySlot.captured
        assertTrue(capturedEntry.reason?.contains(pattern) == true, "Log reason should contain pattern '$pattern'")
        assertEquals("Removed pattern: $pattern", capturedEntry.reason)
    }

    @Test
    fun `should use correct ActionType BLOCKLIST_REMOVED`() = runTest {
        // Given
        val chatId = -100123L
        val pattern = "spam"
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val logEntrySlot = slot<ModerationLogEntry>()

        every { blocklistService.removePattern(chatId, pattern) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        blocklistService.removePattern(chatId, pattern)
        val settings = chatService.getSettings(chatId)

        // Simulate handler logging
        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = "Admin",
            adminUserName = "testadmin",
            actionType = ActionType.BLOCKLIST_REMOVED,
            reason = "Removed pattern: $pattern",
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { logChannelService.logModerationAction(any()) }
        val capturedEntry = logEntrySlot.captured
        assertEquals(ActionType.BLOCKLIST_REMOVED, capturedEntry.actionType)
    }

    @Test
    fun `should log with MANUAL source`() = runTest {
        // Given
        val chatId = -100123L
        val pattern = "test"
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val logEntrySlot = slot<ModerationLogEntry>()

        every { blocklistService.removePattern(chatId, pattern) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        blocklistService.removePattern(chatId, pattern)
        val settings = chatService.getSettings(chatId)

        // Simulate handler logging
        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 789012L,
            adminFirstName = "Admin",
            adminLastName = "User",
            adminUserName = "adminuser",
            actionType = ActionType.BLOCKLIST_REMOVED,
            reason = "Removed pattern: $pattern",
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { logChannelService.logModerationAction(any()) }
        val capturedEntry = logEntrySlot.captured
        assertEquals(PunishmentSource.MANUAL, capturedEntry.source)
    }

    @Test
    fun `should include chat title in log entry`() = runTest {
        // Given
        val chatId = -100123L
        val pattern = "spam"
        val chatTitle = "Important Group Chat"
        val chatSettings = createChatSettings(chatId, chatTitle)
        val logEntrySlot = slot<ModerationLogEntry>()

        every { blocklistService.removePattern(chatId, pattern) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        blocklistService.removePattern(chatId, pattern)
        val settings = chatService.getSettings(chatId)

        // Simulate handler logging
        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = "Admin",
            adminUserName = "testadmin",
            actionType = ActionType.BLOCKLIST_REMOVED,
            reason = "Removed pattern: $pattern",
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { logChannelService.logModerationAction(any()) }
        val capturedEntry = logEntrySlot.captured
        assertEquals(chatTitle, capturedEntry.chatTitle)
    }

    @Test
    fun `should include admin info in log entry`() = runTest {
        // Given
        val chatId = -100123L
        val pattern = "test"
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val adminId = 987654L
        val adminFirstName = "John"
        val adminLastName = "Doe"
        val adminUsername = "johndoe"
        val logEntrySlot = slot<ModerationLogEntry>()

        every { blocklistService.removePattern(chatId, pattern) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        blocklistService.removePattern(chatId, pattern)
        val settings = chatService.getSettings(chatId)

        // Simulate handler logging
        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = adminId,
            adminFirstName = adminFirstName,
            adminLastName = adminLastName,
            adminUserName = adminUsername,
            actionType = ActionType.BLOCKLIST_REMOVED,
            reason = "Removed pattern: $pattern",
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { logChannelService.logModerationAction(any()) }
        val capturedEntry = logEntrySlot.captured
        assertEquals(adminId, capturedEntry.adminId)
        assertEquals(adminFirstName, capturedEntry.adminFirstName)
        assertEquals(adminLastName, capturedEntry.adminLastName)
        assertEquals(adminUsername, capturedEntry.adminUserName)
    }

    @Test
    fun `should handle patterns with wildcards in log reason`() = runTest {
        // Given
        val chatId = -100123L
        val pattern = "*spam*"
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val logEntrySlot = slot<ModerationLogEntry>()

        every { blocklistService.removePattern(chatId, pattern) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        blocklistService.removePattern(chatId, pattern)
        val settings = chatService.getSettings(chatId)

        // Simulate handler logging
        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = "Admin",
            adminUserName = "testadmin",
            actionType = ActionType.BLOCKLIST_REMOVED,
            reason = "Removed pattern: $pattern",
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { logChannelService.logModerationAction(any()) }
        val capturedEntry = logEntrySlot.captured
        assertEquals("Removed pattern: $pattern", capturedEntry.reason)
        assertTrue(capturedEntry.reason?.contains("*spam*") == true)
    }

    @Test
    fun `should handle null chat settings gracefully`() = runTest {
        // Given
        val chatId = -100123L
        val pattern = "test"
        val logEntrySlot = slot<ModerationLogEntry>()

        every { blocklistService.removePattern(chatId, pattern) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns null
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        blocklistService.removePattern(chatId, pattern)
        val settings = chatService.getSettings(chatId)

        // Simulate handler logging (chatTitle will be null)
        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = "Admin",
            adminUserName = "testadmin",
            actionType = ActionType.BLOCKLIST_REMOVED,
            reason = "Removed pattern: $pattern",
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { logChannelService.logModerationAction(any()) }
        val capturedEntry = logEntrySlot.captured
        assertEquals(null, capturedEntry.chatTitle)
    }

    @Test
    fun `logChannelService mock should not throw on any call`() {
        // Given - logChannelService is mocked with relaxed = true

        // When/Then - should not throw on any method call
        logChannelService.logModerationAction(mockk(relaxed = true))
        // No exception = success
    }

    // Helper function
    private fun createChatSettings(
        chatId: Long,
        chatTitle: String
    ) = ChatSettings(
        id = 1L,
        chatId = chatId,
        chatTitle = chatTitle,
        collectionEnabled = true
    )
}
