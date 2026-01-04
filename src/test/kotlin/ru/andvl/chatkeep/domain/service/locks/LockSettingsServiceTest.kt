package ru.andvl.chatkeep.domain.service.locks

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.locks.AllowlistType
import ru.andvl.chatkeep.domain.model.locks.ExemptionType
import ru.andvl.chatkeep.domain.model.locks.LockAllowlist
import ru.andvl.chatkeep.domain.model.locks.LockConfig
import ru.andvl.chatkeep.domain.model.locks.LockExemption
import ru.andvl.chatkeep.domain.model.locks.LockSettings
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.infrastructure.repository.locks.LockAllowlistRepository
import ru.andvl.chatkeep.infrastructure.repository.locks.LockExemptionRepository
import ru.andvl.chatkeep.infrastructure.repository.locks.LockSettingsRepository
import java.util.Optional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for LockSettingsService.
 * Tests lock management, warnings, exemptions, and allowlists.
 */
class LockSettingsServiceTest {

    private lateinit var lockSettingsRepository: LockSettingsRepository
    private lateinit var lockExemptionRepository: LockExemptionRepository
    private lateinit var lockAllowlistRepository: LockAllowlistRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var service: LockSettingsService

    @BeforeEach
    fun setup() {
        lockSettingsRepository = mockk()
        lockExemptionRepository = mockk()
        lockAllowlistRepository = mockk()
        objectMapper = ObjectMapper()
        service = LockSettingsService(
            lockSettingsRepository,
            lockExemptionRepository,
            lockAllowlistRepository,
            objectMapper
        )
    }

    // LOCK MANAGEMENT TESTS

    @Test
    fun `setLock should create new settings when none exist and lock is enabled`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO
        val reason = "No photos allowed"

        every { lockSettingsRepository.findById(chatId) } returns Optional.empty()

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        service.setLock(chatId, lockType, locked = true, reason = reason)

        // Then
        verify { lockSettingsRepository.save(any()) }

        val savedSettings = settingsSlot.captured
        assertEquals(chatId, savedSettings.chatId)
        assertNotNull(savedSettings.locksJson)

        // Verify JSON contains the lock
        val locks = objectMapper.readValue(
            savedSettings.locksJson,
            object : tools.jackson.core.type.TypeReference<Map<String, LockConfig>>() {}
        )
        assertTrue(locks.containsKey(lockType.name))
        assertEquals(true, locks[lockType.name]?.locked)
        assertEquals(reason, locks[lockType.name]?.reason)
    }

    @Test
    fun `setLock should add lock to existing settings when lock is enabled`() {
        // Given
        val chatId = 123L
        val lockType = LockType.VIDEO
        val reason = "Videos prohibited"

        val existingSettings = LockSettings(
            chatId = chatId,
            locksJson = """{"PHOTO":{"locked":true,"reason":"No photos"}}"""
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(existingSettings)

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        service.setLock(chatId, lockType, locked = true, reason = reason)

        // Then
        val savedSettings = settingsSlot.captured
        val locks = objectMapper.readValue(
            savedSettings.locksJson,
            object : tools.jackson.core.type.TypeReference<Map<String, LockConfig>>() {}
        )

        // Should have both locks
        assertEquals(2, locks.size)
        assertTrue(locks.containsKey("PHOTO"))
        assertTrue(locks.containsKey("VIDEO"))
        assertEquals(reason, locks["VIDEO"]?.reason)
    }

    @Test
    fun `setLock should remove lock when locked is false`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO

        val existingSettings = LockSettings(
            chatId = chatId,
            locksJson = """{"PHOTO":{"locked":true,"reason":"test"},"VIDEO":{"locked":true}}"""
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(existingSettings)

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        service.setLock(chatId, lockType, locked = false)

        // Then
        val savedSettings = settingsSlot.captured
        val locks = objectMapper.readValue(
            savedSettings.locksJson,
            object : tools.jackson.core.type.TypeReference<Map<String, LockConfig>>() {}
        )

        // PHOTO should be removed, VIDEO should remain
        assertEquals(1, locks.size)
        assertFalse(locks.containsKey("PHOTO"))
        assertTrue(locks.containsKey("VIDEO"))
    }

    @Test
    fun `setLock should support lock without reason`() {
        // Given
        val chatId = 123L
        val lockType = LockType.STICKER

        every { lockSettingsRepository.findById(chatId) } returns Optional.empty()

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        service.setLock(chatId, lockType, locked = true, reason = null)

        // Then
        val savedSettings = settingsSlot.captured
        val locks = objectMapper.readValue(
            savedSettings.locksJson,
            object : tools.jackson.core.type.TypeReference<Map<String, LockConfig>>() {}
        )

        assertEquals(true, locks[lockType.name]?.locked)
        assertNull(locks[lockType.name]?.reason)
    }

    @Test
    fun `setLock should update existing lock with new reason`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO
        val newReason = "Updated reason"

        val existingSettings = LockSettings(
            chatId = chatId,
            locksJson = """{"PHOTO":{"locked":true,"reason":"old reason"}}"""
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(existingSettings)

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        service.setLock(chatId, lockType, locked = true, reason = newReason)

        // Then
        val savedSettings = settingsSlot.captured
        val locks = objectMapper.readValue(
            savedSettings.locksJson,
            object : tools.jackson.core.type.TypeReference<Map<String, LockConfig>>() {}
        )

        assertEquals(newReason, locks[lockType.name]?.reason)
    }

    @Test
    fun `getLock should return lock config when lock exists`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO
        val reason = "Test reason"

        val settings = LockSettings(
            chatId = chatId,
            locksJson = """{"PHOTO":{"locked":true,"reason":"$reason"}}"""
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(settings)

        // When
        val result = service.getLock(chatId, lockType)

        // Then
        assertNotNull(result)
        assertEquals(true, result.locked)
        assertEquals(reason, result.reason)
    }

    @Test
    fun `getLock should return null when lock does not exist`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO

        val settings = LockSettings(
            chatId = chatId,
            locksJson = """{"VIDEO":{"locked":true}}"""
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(settings)

        // When
        val result = service.getLock(chatId, lockType)

        // Then
        assertNull(result)
    }

    @Test
    fun `getLock should return null when settings do not exist`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO

        every { lockSettingsRepository.findById(chatId) } returns Optional.empty()

        // When
        val result = service.getLock(chatId, lockType)

        // Then
        assertNull(result)
    }

    @Test
    fun `getAllLocks should return empty map when no settings exist`() {
        // Given
        val chatId = 123L

        every { lockSettingsRepository.findById(chatId) } returns Optional.empty()

        // When
        val result = service.getAllLocks(chatId)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllLocks should return all locks with typed keys`() {
        // Given
        val chatId = 123L

        val settings = LockSettings(
            chatId = chatId,
            locksJson = """{"PHOTO":{"locked":true,"reason":"reason1"},"VIDEO":{"locked":true},"STICKER":{"locked":true}}"""
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(settings)

        // When
        val result = service.getAllLocks(chatId)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.containsKey(LockType.PHOTO))
        assertTrue(result.containsKey(LockType.VIDEO))
        assertTrue(result.containsKey(LockType.STICKER))
        assertEquals("reason1", result[LockType.PHOTO]?.reason)
    }

    @Test
    fun `getAllLocks should handle empty locks JSON`() {
        // Given
        val chatId = 123L

        val settings = LockSettings(
            chatId = chatId,
            locksJson = "{}"
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(settings)

        // When
        val result = service.getAllLocks(chatId)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllLocks should skip unknown lock types gracefully`() {
        // Given
        val chatId = 123L

        val settings = LockSettings(
            chatId = chatId,
            locksJson = """{"PHOTO":{"locked":true},"UNKNOWN_TYPE":{"locked":true},"VIDEO":{"locked":true}}"""
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(settings)

        // When
        val result = service.getAllLocks(chatId)

        // Then
        // Should skip UNKNOWN_TYPE but include valid types
        assertEquals(2, result.size)
        assertTrue(result.containsKey(LockType.PHOTO))
        assertTrue(result.containsKey(LockType.VIDEO))
    }

    // LOCK WARNS TESTS

    @Test
    fun `isLockWarnsEnabled should return false when settings do not exist`() {
        // Given
        val chatId = 123L

        every { lockSettingsRepository.findById(chatId) } returns Optional.empty()

        // When
        val result = service.isLockWarnsEnabled(chatId)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isLockWarnsEnabled should return false when lock warns is disabled`() {
        // Given
        val chatId = 123L

        val settings = LockSettings(chatId = chatId, lockWarns = false)
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(settings)

        // When
        val result = service.isLockWarnsEnabled(chatId)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isLockWarnsEnabled should return true when lock warns is enabled`() {
        // Given
        val chatId = 123L

        val settings = LockSettings(chatId = chatId, lockWarns = true)
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(settings)

        // When
        val result = service.isLockWarnsEnabled(chatId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `setLockWarns should create settings when none exist and enable warns`() {
        // Given
        val chatId = 123L

        every { lockSettingsRepository.findById(chatId) } returns Optional.empty()

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        service.setLockWarns(chatId, enabled = true)

        // Then
        val savedSettings = settingsSlot.captured
        assertEquals(chatId, savedSettings.chatId)
        assertTrue(savedSettings.lockWarns)
    }

    @Test
    fun `setLockWarns should update existing settings to enable warns`() {
        // Given
        val chatId = 123L

        val existingSettings = LockSettings(chatId = chatId, lockWarns = false)
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(existingSettings)

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        service.setLockWarns(chatId, enabled = true)

        // Then
        assertTrue(settingsSlot.captured.lockWarns)
    }

    @Test
    fun `setLockWarns should update existing settings to disable warns`() {
        // Given
        val chatId = 123L

        val existingSettings = LockSettings(chatId = chatId, lockWarns = true)
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(existingSettings)

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        service.setLockWarns(chatId, enabled = false)

        // Then
        assertFalse(settingsSlot.captured.lockWarns)
    }

    // EXEMPTION TESTS

    @Test
    fun `addExemption should save exemption with lock type`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO
        val exemptionType = ExemptionType.USER
        val value = "12345"

        val exemptionSlot = slot<LockExemption>()
        every { lockExemptionRepository.save(capture(exemptionSlot)) } answers { firstArg() }

        // When
        service.addExemption(chatId, lockType, exemptionType, value)

        // Then
        val savedExemption = exemptionSlot.captured
        assertEquals(chatId, savedExemption.chatId)
        assertEquals(lockType.name, savedExemption.lockType)
        assertEquals(exemptionType.name, savedExemption.exemptionType)
        assertEquals(value, savedExemption.exemptionValue)
    }

    @Test
    fun `addExemption should save global exemption when lock type is null`() {
        // Given
        val chatId = 123L
        val exemptionType = ExemptionType.BOT
        val value = "bot123"

        val exemptionSlot = slot<LockExemption>()
        every { lockExemptionRepository.save(capture(exemptionSlot)) } answers { firstArg() }

        // When
        service.addExemption(chatId, lockType = null, exemptionType, value)

        // Then
        val savedExemption = exemptionSlot.captured
        assertEquals(chatId, savedExemption.chatId)
        assertNull(savedExemption.lockType)
        assertEquals(exemptionType.name, savedExemption.exemptionType)
        assertEquals(value, savedExemption.exemptionValue)
    }

    @Test
    fun `removeExemption should call repository delete method`() {
        // Given
        val chatId = 123L
        val exemptionType = ExemptionType.USER
        val value = "12345"

        justRun {
            lockExemptionRepository.deleteByChatIdAndExemptionTypeAndExemptionValue(
                any(),
                any(),
                any()
            )
        }

        // When
        service.removeExemption(chatId, exemptionType, value)

        // Then
        verify {
            lockExemptionRepository.deleteByChatIdAndExemptionTypeAndExemptionValue(
                chatId = chatId,
                exemptionType = exemptionType.name,
                exemptionValue = value
            )
        }
    }

    @Test
    fun `getExemptions should return all exemptions for chat`() {
        // Given
        val chatId = 123L
        val exemptions = listOf(
            LockExemption(
                id = 1,
                chatId = chatId,
                lockType = LockType.PHOTO.name,
                exemptionType = ExemptionType.USER.name,
                exemptionValue = "123"
            ),
            LockExemption(
                id = 2,
                chatId = chatId,
                lockType = null,
                exemptionType = ExemptionType.BOT.name,
                exemptionValue = "456"
            )
        )

        every { lockExemptionRepository.findAllByChatId(chatId) } returns exemptions

        // When
        val result = service.getExemptions(chatId)

        // Then
        assertEquals(2, result.size)
        assertEquals(exemptions, result)
    }

    @Test
    fun `getExemptions should return empty list when no exemptions exist`() {
        // Given
        val chatId = 123L

        every { lockExemptionRepository.findAllByChatId(chatId) } returns emptyList()

        // When
        val result = service.getExemptions(chatId)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `isExempt should return true when specific lock type exemption exists`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO
        val exemptionType = ExemptionType.USER
        val value = "12345"

        val exemptions = listOf(
            LockExemption(
                chatId = chatId,
                lockType = lockType.name,
                exemptionType = exemptionType.name,
                exemptionValue = value
            )
        )

        every {
            lockExemptionRepository.findAllByChatIdAndLockType(chatId, lockType.name)
        } returns exemptions

        every {
            lockExemptionRepository.findAllByChatIdAndLockType(chatId, null)
        } returns emptyList()

        // When
        val result = service.isExempt(chatId, lockType, exemptionType, value)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isExempt should return true when global exemption exists`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO
        val exemptionType = ExemptionType.USER
        val value = "12345"

        val globalExemptions = listOf(
            LockExemption(
                chatId = chatId,
                lockType = null,
                exemptionType = exemptionType.name,
                exemptionValue = value
            )
        )

        every {
            lockExemptionRepository.findAllByChatIdAndLockType(chatId, lockType.name)
        } returns emptyList()

        every {
            lockExemptionRepository.findAllByChatIdAndLockType(chatId, null)
        } returns globalExemptions

        // When
        val result = service.isExempt(chatId, lockType, exemptionType, value)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isExempt should return false when no exemption exists`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO
        val exemptionType = ExemptionType.USER
        val value = "12345"

        every {
            lockExemptionRepository.findAllByChatIdAndLockType(chatId, lockType.name)
        } returns emptyList()

        every {
            lockExemptionRepository.findAllByChatIdAndLockType(chatId, null)
        } returns emptyList()

        // When
        val result = service.isExempt(chatId, lockType, exemptionType, value)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isExempt should return false when value does not match`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO
        val exemptionType = ExemptionType.USER
        val value = "12345"

        val exemptions = listOf(
            LockExemption(
                chatId = chatId,
                lockType = lockType.name,
                exemptionType = exemptionType.name,
                exemptionValue = "99999" // Different value
            )
        )

        every {
            lockExemptionRepository.findAllByChatIdAndLockType(chatId, lockType.name)
        } returns exemptions

        every {
            lockExemptionRepository.findAllByChatIdAndLockType(chatId, null)
        } returns emptyList()

        // When
        val result = service.isExempt(chatId, lockType, exemptionType, value)

        // Then
        assertFalse(result)
    }

    // ALLOWLIST TESTS

    @Test
    fun `addToAllowlist should save allowlist entry`() {
        // Given
        val chatId = 123L
        val type = AllowlistType.URL
        val pattern = "https://example.com"

        val allowlistSlot = slot<LockAllowlist>()
        every { lockAllowlistRepository.save(capture(allowlistSlot)) } answers { firstArg() }

        // When
        service.addToAllowlist(chatId, type, pattern)

        // Then
        val savedAllowlist = allowlistSlot.captured
        assertEquals(chatId, savedAllowlist.chatId)
        assertEquals(type.name, savedAllowlist.allowlistType)
        assertEquals(pattern, savedAllowlist.pattern)
    }

    @Test
    fun `addToAllowlist should support different allowlist types`() {
        // Given
        val chatId = 123L

        every { lockAllowlistRepository.save(any()) } answers { firstArg() }

        // When/Then
        service.addToAllowlist(chatId, AllowlistType.URL, "https://example.com")
        service.addToAllowlist(chatId, AllowlistType.DOMAIN, "example.com")
        service.addToAllowlist(chatId, AllowlistType.COMMAND, "/start")

        verify(exactly = 3) { lockAllowlistRepository.save(any()) }
    }

    @Test
    fun `removeFromAllowlist should call repository delete method`() {
        // Given
        val chatId = 123L
        val type = AllowlistType.URL
        val pattern = "https://example.com"

        justRun {
            lockAllowlistRepository.deleteByChatIdAndAllowlistTypeAndPattern(any(), any(), any())
        }

        // When
        service.removeFromAllowlist(chatId, type, pattern)

        // Then
        verify {
            lockAllowlistRepository.deleteByChatIdAndAllowlistTypeAndPattern(
                chatId = chatId,
                allowlistType = type.name,
                pattern = pattern
            )
        }
    }

    @Test
    fun `getAllowlist should return list of patterns for specified type`() {
        // Given
        val chatId = 123L
        val type = AllowlistType.URL

        val allowlistEntries = listOf(
            LockAllowlist(
                id = 1,
                chatId = chatId,
                allowlistType = type.name,
                pattern = "https://example.com"
            ),
            LockAllowlist(
                id = 2,
                chatId = chatId,
                allowlistType = type.name,
                pattern = "https://trusted.com"
            )
        )

        every {
            lockAllowlistRepository.findAllByChatIdAndAllowlistType(chatId, type.name)
        } returns allowlistEntries

        // When
        val result = service.getAllowlist(chatId, type)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("https://example.com"))
        assertTrue(result.contains("https://trusted.com"))
    }

    @Test
    fun `getAllowlist should return empty list when no entries exist`() {
        // Given
        val chatId = 123L
        val type = AllowlistType.DOMAIN

        every {
            lockAllowlistRepository.findAllByChatIdAndAllowlistType(chatId, type.name)
        } returns emptyList()

        // When
        val result = service.getAllowlist(chatId, type)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllowlist should only return patterns for specified type`() {
        // Given
        val chatId = 123L

        val urlEntries = listOf(
            LockAllowlist(
                id = 1,
                chatId = chatId,
                allowlistType = AllowlistType.URL.name,
                pattern = "https://example.com"
            )
        )

        val domainEntries = listOf(
            LockAllowlist(
                id = 2,
                chatId = chatId,
                allowlistType = AllowlistType.DOMAIN.name,
                pattern = "example.com"
            )
        )

        every {
            lockAllowlistRepository.findAllByChatIdAndAllowlistType(chatId, AllowlistType.URL.name)
        } returns urlEntries

        every {
            lockAllowlistRepository.findAllByChatIdAndAllowlistType(chatId, AllowlistType.DOMAIN.name)
        } returns domainEntries

        // When
        val urlResults = service.getAllowlist(chatId, AllowlistType.URL)
        val domainResults = service.getAllowlist(chatId, AllowlistType.DOMAIN)

        // Then
        assertEquals(1, urlResults.size)
        assertEquals("https://example.com", urlResults[0])

        assertEquals(1, domainResults.size)
        assertEquals("example.com", domainResults[0])
    }

    // EDGE CASES AND ERROR HANDLING

    @Test
    fun `setLock should handle malformed JSON gracefully`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO

        val existingSettings = LockSettings(
            chatId = chatId,
            locksJson = "INVALID JSON {{{" // Malformed JSON
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(existingSettings)

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        service.setLock(chatId, lockType, locked = true, reason = "test")

        // Then - should create new lock settings
        verify { lockSettingsRepository.save(any()) }
        val savedSettings = settingsSlot.captured

        val locks = objectMapper.readValue(
            savedSettings.locksJson,
            object : tools.jackson.core.type.TypeReference<Map<String, LockConfig>>() {}
        )

        // Should have only the new lock
        assertEquals(1, locks.size)
        assertTrue(locks.containsKey(lockType.name))
    }

    @Test
    fun `getAllLocks should handle malformed JSON gracefully`() {
        // Given
        val chatId = 123L

        val settings = LockSettings(
            chatId = chatId,
            locksJson = "INVALID JSON"
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(settings)

        // When
        val result = service.getAllLocks(chatId)

        // Then - should return empty map
        assertTrue(result.isEmpty())
    }

    @Test
    fun `setLock should update updatedAt timestamp`() {
        // Given
        val chatId = 123L
        val lockType = LockType.PHOTO
        val oldTimestamp = Instant.now().minusSeconds(3600)

        val existingSettings = LockSettings(
            chatId = chatId,
            locksJson = """{"VIDEO":{"locked":true}}""",
            createdAt = oldTimestamp,
            updatedAt = oldTimestamp
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(existingSettings)

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        val before = Instant.now()
        service.setLock(chatId, lockType, locked = true)
        val after = Instant.now()

        // Then
        val savedSettings = settingsSlot.captured
        assertTrue(savedSettings.updatedAt.isAfter(before.minusSeconds(1)))
        assertTrue(savedSettings.updatedAt.isBefore(after.plusSeconds(1)))
    }

    @Test
    fun `setLockWarns should update updatedAt timestamp`() {
        // Given
        val chatId = 123L
        val oldTimestamp = Instant.now().minusSeconds(3600)

        val existingSettings = LockSettings(
            chatId = chatId,
            createdAt = oldTimestamp,
            updatedAt = oldTimestamp
        )
        every { lockSettingsRepository.findById(chatId) } returns Optional.of(existingSettings)

        val settingsSlot = slot<LockSettings>()
        every { lockSettingsRepository.save(capture(settingsSlot)) } answers { firstArg() }

        // When
        val before = Instant.now()
        service.setLockWarns(chatId, enabled = true)
        val after = Instant.now()

        // Then
        val savedSettings = settingsSlot.captured
        assertTrue(savedSettings.updatedAt.isAfter(before.minusSeconds(1)))
        assertTrue(savedSettings.updatedAt.isBefore(after.plusSeconds(1)))
    }

    @Test
    fun `multiple lock types can be set and retrieved independently`() {
        // Given
        val chatId = 123L

        every { lockSettingsRepository.findById(chatId) } returns Optional.empty() andThen
                Optional.of(LockSettings(chatId = chatId, locksJson = """{"PHOTO":{"locked":true}}""")) andThen
                Optional.of(LockSettings(chatId = chatId, locksJson = """{"PHOTO":{"locked":true},"VIDEO":{"locked":true},"STICKER":{"locked":true}}"""))

        every { lockSettingsRepository.save(any()) } answers { firstArg() }

        // When
        service.setLock(chatId, LockType.PHOTO, locked = true)
        service.setLock(chatId, LockType.VIDEO, locked = true)
        service.setLock(chatId, LockType.STICKER, locked = true)

        // Then
        val result = service.getAllLocks(chatId)
        assertEquals(3, result.size)
        assertTrue(result.containsKey(LockType.PHOTO))
        assertTrue(result.containsKey(LockType.VIDEO))
        assertTrue(result.containsKey(LockType.STICKER))
    }
}
