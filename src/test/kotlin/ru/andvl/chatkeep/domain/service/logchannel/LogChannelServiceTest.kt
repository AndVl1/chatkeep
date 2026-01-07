package ru.andvl.chatkeep.domain.service.logchannel

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for LogChannelService.
 * Tests config change logging and lock warns logging.
 */
class LogChannelServiceTest {

    private lateinit var logChannelPort: LogChannelPort
    private lateinit var moderationConfigRepository: ModerationConfigRepository
    private lateinit var service: LogChannelService

    @BeforeEach
    fun setup() {
        logChannelPort = mockk()
        moderationConfigRepository = mockk()
        service = LogChannelService(logChannelPort, moderationConfigRepository)
    }

    // CONFIG CHANGE LOGGING TESTS

    @Test
    fun `logModerationAction should send log when log channel is configured`() {
        // Given
        val chatId = 123L
        val logChannelId = -1001234567890L
        val config = ModerationConfig(chatId = chatId, logChannelId = logChannelId)

        every { moderationConfigRepository.findByChatId(chatId) } returns config
        coEvery { logChannelPort.sendLogEntry(logChannelId, any()) } returns true

        val entry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = "Test Chat",
            adminId = 456L,
            adminFirstName = "Admin",
            adminLastName = null,
            adminUserName = "admin_user",
            actionType = ActionType.CONFIG_CHANGED,
            reason = "maxWarnings: 5, cleanService: ON",
            source = PunishmentSource.MANUAL
        )

        // When
        service.logModerationAction(entry)

        // Then - need to wait for async operation
        Thread.sleep(100)
        coVerify(timeout = 1000) { logChannelPort.sendLogEntry(logChannelId, entry) }
    }

    @Test
    fun `logModerationAction should not send log when no log channel configured`() {
        // Given
        val chatId = 123L
        val config = ModerationConfig(chatId = chatId, logChannelId = null)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        val entry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = "Test Chat",
            adminId = 456L,
            adminFirstName = "Admin",
            adminLastName = null,
            adminUserName = null,
            actionType = ActionType.CONFIG_CHANGED,
            reason = "test change",
            source = PunishmentSource.MANUAL
        )

        // When
        service.logModerationAction(entry)

        // Then
        Thread.sleep(100)
        coVerify(exactly = 0) { logChannelPort.sendLogEntry(any(), any()) }
    }

    @Test
    fun `logModerationAction should not send log when config does not exist`() {
        // Given
        val chatId = 123L

        every { moderationConfigRepository.findByChatId(chatId) } returns null

        val entry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = "Test Chat",
            adminId = 456L,
            adminFirstName = "Admin",
            adminLastName = null,
            adminUserName = null,
            actionType = ActionType.LOCK_WARNS_ON,
            source = PunishmentSource.MANUAL
        )

        // When
        service.logModerationAction(entry)

        // Then
        Thread.sleep(100)
        coVerify(exactly = 0) { logChannelPort.sendLogEntry(any(), any()) }
    }

    // LOCK WARNS LOGGING TESTS

    @Test
    fun `logModerationAction should log LOCK_WARNS_ON action`() {
        // Given
        val chatId = 123L
        val logChannelId = -1001234567890L
        val config = ModerationConfig(chatId = chatId, logChannelId = logChannelId)

        every { moderationConfigRepository.findByChatId(chatId) } returns config
        coEvery { logChannelPort.sendLogEntry(logChannelId, any()) } returns true

        val entry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = "Test Chat",
            adminId = 456L,
            adminFirstName = "Admin",
            adminLastName = "User",
            adminUserName = "admin_user",
            actionType = ActionType.LOCK_WARNS_ON,
            source = PunishmentSource.MANUAL
        )

        // When
        service.logModerationAction(entry)

        // Then
        Thread.sleep(100)
        coVerify(timeout = 1000) {
            logChannelPort.sendLogEntry(logChannelId, match { it.actionType == ActionType.LOCK_WARNS_ON })
        }
    }

    @Test
    fun `logModerationAction should log LOCK_WARNS_OFF action`() {
        // Given
        val chatId = 123L
        val logChannelId = -1001234567890L
        val config = ModerationConfig(chatId = chatId, logChannelId = logChannelId)

        every { moderationConfigRepository.findByChatId(chatId) } returns config
        coEvery { logChannelPort.sendLogEntry(logChannelId, any()) } returns true

        val entry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = "Test Chat",
            adminId = 456L,
            adminFirstName = "Admin",
            adminLastName = null,
            adminUserName = null,
            actionType = ActionType.LOCK_WARNS_OFF,
            source = PunishmentSource.MANUAL
        )

        // When
        service.logModerationAction(entry)

        // Then
        Thread.sleep(100)
        coVerify(timeout = 1000) {
            logChannelPort.sendLogEntry(logChannelId, match { it.actionType == ActionType.LOCK_WARNS_OFF })
        }
    }

    // SET/UNSET LOG CHANNEL TESTS

    @Test
    fun `setLogChannel should create new config when none exists`() {
        // Given
        val chatId = 123L
        val logChannelId = -1001234567890L

        every { moderationConfigRepository.findByChatId(chatId) } returns null

        val configSlot = slot<ModerationConfig>()
        every { moderationConfigRepository.save(capture(configSlot)) } answers { firstArg() }

        // When
        val result = service.setLogChannel(chatId, logChannelId)

        // Then
        assertTrue(result)
        assertEquals(chatId, configSlot.captured.chatId)
        assertEquals(logChannelId, configSlot.captured.logChannelId)
    }

    @Test
    fun `setLogChannel should update existing config`() {
        // Given
        val chatId = 123L
        val oldLogChannelId = -1001111111111L
        val newLogChannelId = -1002222222222L
        val existingConfig = ModerationConfig(chatId = chatId, logChannelId = oldLogChannelId)

        every { moderationConfigRepository.findByChatId(chatId) } returns existingConfig

        val configSlot = slot<ModerationConfig>()
        every { moderationConfigRepository.save(capture(configSlot)) } answers { firstArg() }

        // When
        val result = service.setLogChannel(chatId, newLogChannelId)

        // Then
        assertTrue(result)
        assertEquals(newLogChannelId, configSlot.captured.logChannelId)
    }

    @Test
    fun `unsetLogChannel should return false when no config exists`() {
        // Given
        val chatId = 123L

        every { moderationConfigRepository.findByChatId(chatId) } returns null

        // When
        val result = service.unsetLogChannel(chatId)

        // Then
        assertFalse(result)
        verify(exactly = 0) { moderationConfigRepository.save(any()) }
    }

    @Test
    fun `unsetLogChannel should return false when log channel not set`() {
        // Given
        val chatId = 123L
        val config = ModerationConfig(chatId = chatId, logChannelId = null)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = service.unsetLogChannel(chatId)

        // Then
        assertFalse(result)
        verify(exactly = 0) { moderationConfigRepository.save(any()) }
    }

    @Test
    fun `unsetLogChannel should remove log channel from config`() {
        // Given
        val chatId = 123L
        val logChannelId = -1001234567890L
        val existingConfig = ModerationConfig(chatId = chatId, logChannelId = logChannelId)

        every { moderationConfigRepository.findByChatId(chatId) } returns existingConfig

        val configSlot = slot<ModerationConfig>()
        every { moderationConfigRepository.save(capture(configSlot)) } answers { firstArg() }

        // When
        val result = service.unsetLogChannel(chatId)

        // Then
        assertTrue(result)
        assertNull(configSlot.captured.logChannelId)
    }

    @Test
    fun `getLogChannel should return null when no config exists`() {
        // Given
        val chatId = 123L

        every { moderationConfigRepository.findByChatId(chatId) } returns null

        // When
        val result = service.getLogChannel(chatId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getLogChannel should return log channel id when configured`() {
        // Given
        val chatId = 123L
        val logChannelId = -1001234567890L
        val config = ModerationConfig(chatId = chatId, logChannelId = logChannelId)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = service.getLogChannel(chatId)

        // Then
        assertEquals(logChannelId, result)
    }

    // VALIDATE CHANNEL TESTS

    @Test
    fun `validateChannel should return true when channel is accessible`() = runBlocking {
        // Given
        val channelId = -1001234567890L

        coEvery { logChannelPort.validateChannel(channelId) } returns true

        // When
        val result = service.validateChannel(channelId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `validateChannel should return false when channel is not accessible`() = runBlocking {
        // Given
        val channelId = -1001234567890L

        coEvery { logChannelPort.validateChannel(channelId) } returns false

        // When
        val result = service.validateChannel(channelId)

        // Then
        assertFalse(result)
    }
}
