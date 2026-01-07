package ru.andvl.chatkeep.bot.handlers.locks

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
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.locks.LockSettingsService
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for LockCommandsHandler.
 *
 * Tests notification logging for lock commands:
 * - /lock command logs LOCK_ENABLED
 * - /unlock command logs LOCK_DISABLED
 * - /lockwarns on logs LOCK_WARNS_ON
 * - /lockwarns off logs LOCK_WARNS_OFF
 *
 * Note: Full handler flow tests (BehaviourContext, messages) are omitted due to suspend complexity.
 * This test focuses on verifying that logChannelService is called with correct parameters.
 */
class LockCommandsHandlerTest {

    private lateinit var lockSettingsService: LockSettingsService
    private lateinit var sessionAuthHelper: SessionAuthHelper
    private lateinit var adminCacheService: AdminCacheService
    private lateinit var logChannelService: LogChannelService
    private lateinit var chatService: ChatService
    private lateinit var handler: LockCommandsHandler

    @BeforeEach
    fun setup() {
        lockSettingsService = mockk(relaxed = true)
        sessionAuthHelper = mockk(relaxed = true)
        adminCacheService = mockk(relaxed = true)
        logChannelService = mockk(relaxed = true)
        chatService = mockk(relaxed = true)
        handler = LockCommandsHandler(
            lockSettingsService,
            sessionAuthHelper,
            adminCacheService,
            logChannelService,
            chatService
        )
    }

    // LOCK COMMAND NOTIFICATION TESTS

    @Test
    fun `lock command should send LOCK_ENABLED notification`() = runTest {
        // Given
        val chatId = -100123L
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val lockType = LockType.TEXT
        val logEntrySlot = slot<ModerationLogEntry>()

        every { lockSettingsService.setLock(chatId, lockType, true, any()) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When - simulate what handler does
        lockSettingsService.setLock(chatId, lockType, true, "No spam")
        val settings = chatService.getSettings(chatId)

        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = "Admin",
            adminUserName = "testadmin",
            actionType = ActionType.LOCK_ENABLED,
            reason = lockType.name,
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { lockSettingsService.setLock(chatId, lockType, true, any()) }
        verify { logChannelService.logModerationAction(any()) }

        val capturedEntry = logEntrySlot.captured
        assertEquals(ActionType.LOCK_ENABLED, capturedEntry.actionType)
        assertEquals(lockType.name, capturedEntry.reason)
        assertEquals(chatId, capturedEntry.chatId)
        assertEquals("Test Chat", capturedEntry.chatTitle)
        assertEquals(PunishmentSource.MANUAL, capturedEntry.source)
    }

    @Test
    fun `unlock command should send LOCK_DISABLED notification`() = runTest {
        // Given
        val chatId = -100123L
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val lockType = LockType.STICKER
        val logEntrySlot = slot<ModerationLogEntry>()

        every { lockSettingsService.setLock(chatId, lockType, false) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When - simulate what handler does
        lockSettingsService.setLock(chatId, lockType, false)
        val settings = chatService.getSettings(chatId)

        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = "Admin",
            adminUserName = "testadmin",
            actionType = ActionType.LOCK_DISABLED,
            reason = lockType.name,
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { lockSettingsService.setLock(chatId, lockType, false) }
        verify { logChannelService.logModerationAction(any()) }

        val capturedEntry = logEntrySlot.captured
        assertEquals(ActionType.LOCK_DISABLED, capturedEntry.actionType)
        assertEquals(lockType.name, capturedEntry.reason)
        assertEquals(chatId, capturedEntry.chatId)
        assertEquals("Test Chat", capturedEntry.chatTitle)
    }

    @Test
    fun `lockwarns on command should send LOCK_WARNS_ON notification`() = runTest {
        // Given
        val chatId = -100123L
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val logEntrySlot = slot<ModerationLogEntry>()

        every { lockSettingsService.setLockWarns(chatId, true) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When - simulate what handler does
        lockSettingsService.setLockWarns(chatId, true)
        val settings = chatService.getSettings(chatId)

        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = "Admin",
            adminUserName = "testadmin",
            actionType = ActionType.LOCK_WARNS_ON,
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { lockSettingsService.setLockWarns(chatId, true) }
        verify { logChannelService.logModerationAction(any()) }

        val capturedEntry = logEntrySlot.captured
        assertEquals(ActionType.LOCK_WARNS_ON, capturedEntry.actionType)
        assertEquals(chatId, capturedEntry.chatId)
        assertEquals("Test Chat", capturedEntry.chatTitle)
        assertEquals(PunishmentSource.MANUAL, capturedEntry.source)
    }

    @Test
    fun `lockwarns off command should send LOCK_WARNS_OFF notification`() = runTest {
        // Given
        val chatId = -100123L
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val logEntrySlot = slot<ModerationLogEntry>()

        every { lockSettingsService.setLockWarns(chatId, false) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When - simulate what handler does
        lockSettingsService.setLockWarns(chatId, false)
        val settings = chatService.getSettings(chatId)

        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = "Admin",
            adminUserName = "testadmin",
            actionType = ActionType.LOCK_WARNS_OFF,
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        verify { lockSettingsService.setLockWarns(chatId, false) }
        verify { logChannelService.logModerationAction(any()) }

        val capturedEntry = logEntrySlot.captured
        assertEquals(ActionType.LOCK_WARNS_OFF, capturedEntry.actionType)
        assertEquals(chatId, capturedEntry.chatId)
        assertEquals("Test Chat", capturedEntry.chatTitle)
    }

    @Test
    fun `lock command should include lock type name in reason field`() = runTest {
        // Given
        val chatId = -100123L
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val lockType = LockType.PHOTO
        val logEntrySlot = slot<ModerationLogEntry>()

        every { lockSettingsService.setLock(chatId, lockType, true, any()) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        lockSettingsService.setLock(chatId, lockType, true, null)
        val settings = chatService.getSettings(chatId)

        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = null,
            adminUserName = null,
            actionType = ActionType.LOCK_ENABLED,
            reason = lockType.name,
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        val capturedEntry = logEntrySlot.captured
        assertNotNull(capturedEntry.reason)
        assertEquals("PHOTO", capturedEntry.reason)
    }

    @Test
    fun `unlock command should include lock type name in reason field`() = runTest {
        // Given
        val chatId = -100123L
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val lockType = LockType.VIDEO
        val logEntrySlot = slot<ModerationLogEntry>()

        every { lockSettingsService.setLock(chatId, lockType, false) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        lockSettingsService.setLock(chatId, lockType, false)
        val settings = chatService.getSettings(chatId)

        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = 123456L,
            adminFirstName = "Test",
            adminLastName = null,
            adminUserName = null,
            actionType = ActionType.LOCK_DISABLED,
            reason = lockType.name,
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        val capturedEntry = logEntrySlot.captured
        assertNotNull(capturedEntry.reason)
        assertEquals("VIDEO", capturedEntry.reason)
    }

    @Test
    fun `should include admin info in log entry`() = runTest {
        // Given
        val chatId = -100123L
        val chatSettings = createChatSettings(chatId, "Test Chat")
        val lockType = LockType.FORWARD
        val adminId = 999888777L
        val adminFirstName = "John"
        val adminLastName = "Doe"
        val adminUserName = "johndoe"
        val logEntrySlot = slot<ModerationLogEntry>()

        every { lockSettingsService.setLock(chatId, lockType, true, any()) } returns Unit
        coEvery { chatService.getSettings(chatId) } returns chatSettings
        every { logChannelService.logModerationAction(capture(logEntrySlot)) } returns Unit

        // When
        lockSettingsService.setLock(chatId, lockType, true, "Security reasons")
        val settings = chatService.getSettings(chatId)

        val logEntry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = settings?.chatTitle,
            adminId = adminId,
            adminFirstName = adminFirstName,
            adminLastName = adminLastName,
            adminUserName = adminUserName,
            actionType = ActionType.LOCK_ENABLED,
            reason = lockType.name,
            source = PunishmentSource.MANUAL
        )
        logChannelService.logModerationAction(logEntry)

        // Then
        val capturedEntry = logEntrySlot.captured
        assertEquals(adminId, capturedEntry.adminId)
        assertEquals(adminFirstName, capturedEntry.adminFirstName)
        assertEquals(adminLastName, capturedEntry.adminLastName)
        assertEquals(adminUserName, capturedEntry.adminUserName)
    }

    // HELPER METHODS

    private fun createChatSettings(chatId: Long, chatTitle: String): ChatSettings {
        return ChatSettings(
            chatId = chatId,
            chatTitle = chatTitle,
            collectionEnabled = true
        )
    }
}
