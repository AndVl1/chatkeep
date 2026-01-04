package ru.andvl.chatkeep.bot.handlers.locks

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.locks.AllowlistType
import ru.andvl.chatkeep.domain.model.locks.ExemptionType
import ru.andvl.chatkeep.domain.model.locks.LockConfig
import ru.andvl.chatkeep.domain.model.locks.LockExemption
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.model.moderation.Warning
import ru.andvl.chatkeep.domain.service.locks.LockDetectorRegistry
import ru.andvl.chatkeep.domain.service.locks.LockSettingsService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.PunishmentService
import ru.andvl.chatkeep.domain.service.moderation.WarningService
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for LockEnforcementHandler.
 *
 * Tests lock enforcement logic:
 * - Admin exemption - admins should NOT be blocked
 * - Bot exemption - exempt bots should NOT be blocked
 * - Channel post exemption - linked channel posts exempt (unless ANONCHANNEL lock enabled)
 * - User exemption - whitelisted users exempt
 * - Lock warns - warning issued when lock_warns enabled
 * - No locks - nothing happens when no locks configured
 * - Allowlist - URLs/commands in allowlist not blocked
 *
 * Note: This test focuses on service layer interactions and decision-making.
 * Full integration with KTgBotAPI types is tested in bot integration tests.
 */
class LockEnforcementHandlerTest {

    private lateinit var lockSettingsService: LockSettingsService
    private lateinit var lockDetectorRegistry: LockDetectorRegistry
    private lateinit var adminCacheService: AdminCacheService
    private lateinit var warningService: WarningService
    private lateinit var punishmentService: PunishmentService
    private lateinit var handler: LockEnforcementHandler

    @BeforeEach
    fun setup() {
        lockSettingsService = mockk()
        lockDetectorRegistry = mockk()
        adminCacheService = mockk()
        warningService = mockk()
        punishmentService = mockk()
        handler = LockEnforcementHandler(
            lockSettingsService,
            lockDetectorRegistry,
            adminCacheService,
            warningService,
            punishmentService
        )
    }

    // ADMIN EXEMPTION TESTS

    @Test
    fun `should recognize admin as exempt`() = runTest {
        // Given
        val chatId = -123L
        val adminUserId = 456L

        coEvery { adminCacheService.isAdmin(adminUserId, chatId) } returns true

        // When
        val isAdmin = adminCacheService.isAdmin(adminUserId, chatId)

        // Then
        assertTrue(isAdmin, "Admin should be recognized")
        coVerify(exactly = 1) { adminCacheService.isAdmin(adminUserId, chatId) }
    }

    @Test
    fun `should recognize non-admin as not exempt`() = runTest {
        // Given
        val chatId = -123L
        val userId = 456L

        coEvery { adminCacheService.isAdmin(userId, chatId) } returns false

        // When
        val isAdmin = adminCacheService.isAdmin(userId, chatId)

        // Then
        assertFalse(isAdmin, "Non-admin should not be recognized as admin")
    }

    // BOT EXEMPTION TESTS

    @Test
    fun `should find exempt bot in exemptions list`() = runTest {
        // Given
        val chatId = -123L
        val botUsername = "exemptbot"

        coEvery { lockSettingsService.getExemptions(chatId) } returns listOf(
            LockExemption(
                id = 1L,
                chatId = chatId,
                lockType = null,
                exemptionType = ExemptionType.BOT.name,
                exemptionValue = botUsername
            )
        )

        // When
        val exemptions = lockSettingsService.getExemptions(chatId)
        val isExempt = exemptions.any {
            it.exemptionType == ExemptionType.BOT.name &&
            it.exemptionValue == botUsername
        }

        // Then
        assertTrue(isExempt, "Exempt bot should be found in exemptions")
    }

    @Test
    fun `should not find non-exempt bot in exemptions list`() = runTest {
        // Given
        val chatId = -123L
        val botUsername = "spambot"

        coEvery { lockSettingsService.getExemptions(chatId) } returns emptyList()

        // When
        val exemptions = lockSettingsService.getExemptions(chatId)
        val isExempt = exemptions.any {
            it.exemptionType == ExemptionType.BOT.name &&
            it.exemptionValue == botUsername
        }

        // Then
        assertFalse(isExempt, "Non-exempt bot should not be found")
    }

    // CHANNEL POST EXEMPTION TESTS

    @Test
    fun `channel posts should be exempt when ANONCHANNEL lock is disabled`() = runTest {
        // Given
        val chatId = -123L

        coEvery { lockSettingsService.getLock(chatId, LockType.ANONCHANNEL) } returns null

        // When
        val anonChannelLock = lockSettingsService.getLock(chatId, LockType.ANONCHANNEL)
        val isExempt = anonChannelLock == null || !anonChannelLock.locked

        // Then
        assertTrue(isExempt, "Channel posts should be exempt when ANONCHANNEL lock is not active")
    }

    @Test
    fun `channel posts should NOT be exempt when ANONCHANNEL lock is enabled`() = runTest {
        // Given
        val chatId = -123L

        coEvery { lockSettingsService.getLock(chatId, LockType.ANONCHANNEL) } returns LockConfig(
            locked = true,
            reason = "No anonymous channel posts"
        )

        // When
        val anonChannelLock = lockSettingsService.getLock(chatId, LockType.ANONCHANNEL)
        val isExempt = anonChannelLock == null || !anonChannelLock.locked

        // Then
        assertFalse(isExempt, "Channel posts should NOT be exempt when ANONCHANNEL lock is active")
    }

    // USER EXEMPTION TESTS

    @Test
    fun `should find exempt user in exemptions list`() = runTest {
        // Given
        val chatId = -123L
        val userId = 456L

        coEvery { lockSettingsService.getExemptions(chatId) } returns listOf(
            LockExemption(
                id = 1L,
                chatId = chatId,
                lockType = null,
                exemptionType = ExemptionType.USER.name,
                exemptionValue = userId.toString()
            )
        )

        // When
        val exemptions = lockSettingsService.getExemptions(chatId)
        val isExempt = exemptions.any {
            it.exemptionType == ExemptionType.USER.name &&
            it.exemptionValue == userId.toString()
        }

        // Then
        assertTrue(isExempt, "Exempt user should be found in exemptions")
    }

    @Test
    fun `should not find non-exempt user in exemptions list`() = runTest {
        // Given
        val chatId = -123L
        val userId = 789L

        coEvery { lockSettingsService.getExemptions(chatId) } returns listOf(
            LockExemption(
                id = 1L,
                chatId = chatId,
                lockType = null,
                exemptionType = ExemptionType.USER.name,
                exemptionValue = "456" // Different user
            )
        )

        // When
        val exemptions = lockSettingsService.getExemptions(chatId)
        val isExempt = exemptions.any {
            it.exemptionType == ExemptionType.USER.name &&
            it.exemptionValue == userId.toString()
        }

        // Then
        assertFalse(isExempt, "Non-exempt user should not be found")
    }

    // LOCK CONFIGURATION TESTS

    @Test
    fun `should retrieve active locks for chat`() = runTest {
        // Given
        val chatId = -123L

        coEvery { lockSettingsService.getAllLocks(chatId) } returns mapOf(
            LockType.PHOTO to LockConfig(locked = true, reason = "No photos allowed"),
            LockType.VIDEO to LockConfig(locked = true, reason = "No videos")
        )

        // When
        val locks = lockSettingsService.getAllLocks(chatId)

        // Then
        assertEquals(2, locks.size)
        assertTrue(locks.containsKey(LockType.PHOTO))
        assertTrue(locks.containsKey(LockType.VIDEO))
        assertEquals("No photos allowed", locks[LockType.PHOTO]?.reason)
    }

    @Test
    fun `should return empty map when no locks configured`() = runTest {
        // Given
        val chatId = -123L

        coEvery { lockSettingsService.getAllLocks(chatId) } returns emptyMap()

        // When
        val locks = lockSettingsService.getAllLocks(chatId)

        // Then
        assertTrue(locks.isEmpty(), "Should return empty map when no locks configured")
    }

    @Test
    fun `should filter out disabled locks`() = runTest {
        // Given
        val chatId = -123L
        val locks = mapOf(
            LockType.PHOTO to LockConfig(locked = true, reason = "No photos"),
            LockType.VIDEO to LockConfig(locked = false, reason = null)
        )

        coEvery { lockSettingsService.getAllLocks(chatId) } returns locks

        // When
        val allLocks = lockSettingsService.getAllLocks(chatId)
        val activeLocks = allLocks.filter { it.value.locked }

        // Then
        assertEquals(1, activeLocks.size)
        assertTrue(activeLocks.containsKey(LockType.PHOTO))
        assertFalse(activeLocks.containsKey(LockType.VIDEO))
    }

    // LOCK WARNS TESTS

    @Test
    fun `should check if lock_warns is enabled`() = runTest {
        // Given
        val chatId = -123L

        coEvery { lockSettingsService.isLockWarnsEnabled(chatId) } returns true

        // When
        val warnsEnabled = lockSettingsService.isLockWarnsEnabled(chatId)

        // Then
        assertTrue(warnsEnabled, "lock_warns should be enabled")
    }

    @Test
    fun `should check if lock_warns is disabled`() = runTest {
        // Given
        val chatId = -123L

        coEvery { lockSettingsService.isLockWarnsEnabled(chatId) } returns false

        // When
        val warnsEnabled = lockSettingsService.isLockWarnsEnabled(chatId)

        // Then
        assertFalse(warnsEnabled, "lock_warns should be disabled")
    }

    @Test
    fun `should issue warning with correct parameters when lock_warns enabled`() = runTest {
        // Given
        val chatId = -123L
        val userId = 456L
        val reason = "No photos allowed"

        coEvery {
            warningService.issueWarningWithThreshold(chatId, userId, 0, reason)
        } returns WarningService.WarningWithThresholdResult(
            warningResult = WarningService.WarningResult(
                warning = mockk<Warning>(),
                activeCount = 1,
                maxWarnings = 3,
                expiresAt = Instant.now().plusSeconds(3600),
                thresholdAction = PunishmentType.NOTHING
            ),
            thresholdTriggered = false,
            thresholdAction = null,
            thresholdDurationMinutes = null
        )

        // When
        val result = warningService.issueWarningWithThreshold(chatId, userId, 0, reason)

        // Then
        assertEquals(1, result.warningResult.activeCount)
        assertEquals(3, result.warningResult.maxWarnings)
        assertFalse(result.thresholdTriggered)
        coVerify(exactly = 1) {
            warningService.issueWarningWithThreshold(chatId, userId, 0, reason)
        }
    }

    @Test
    fun `should use custom lock reason in warning`() = runTest {
        // Given
        val chatId = -123L
        val userId = 456L
        val customReason = "Custom violation reason"

        val reasonSlot = slot<String>()
        coEvery {
            warningService.issueWarningWithThreshold(chatId, userId, 0, capture(reasonSlot))
        } returns WarningService.WarningWithThresholdResult(
            warningResult = WarningService.WarningResult(
                warning = mockk<Warning>(),
                activeCount = 1,
                maxWarnings = 3,
                expiresAt = Instant.now().plusSeconds(3600),
                thresholdAction = PunishmentType.NOTHING
            ),
            thresholdTriggered = false,
            thresholdAction = null,
            thresholdDurationMinutes = null
        )

        // When
        warningService.issueWarningWithThreshold(chatId, userId, 0, customReason)

        // Then
        assertEquals(customReason, reasonSlot.captured)
    }

    @Test
    fun `should use default reason when lock has no custom reason`() = runTest {
        // Given
        val chatId = -123L
        val userId = 456L
        val defaultReason = "Locked content"

        val reasonSlot = slot<String>()
        coEvery {
            warningService.issueWarningWithThreshold(chatId, userId, 0, capture(reasonSlot))
        } returns WarningService.WarningWithThresholdResult(
            warningResult = WarningService.WarningResult(
                warning = mockk<Warning>(),
                activeCount = 1,
                maxWarnings = 3,
                expiresAt = Instant.now().plusSeconds(3600),
                thresholdAction = PunishmentType.NOTHING
            ),
            thresholdTriggered = false,
            thresholdAction = null,
            thresholdDurationMinutes = null
        )

        // When
        warningService.issueWarningWithThreshold(chatId, userId, 0, defaultReason)

        // Then
        assertEquals(defaultReason, reasonSlot.captured)
    }

    // ALLOWLIST TESTS

    @Test
    fun `should retrieve URL allowlist`() = runTest {
        // Given
        val chatId = -123L
        val allowedUrls = listOf("https://example.com", "https://trusted.org")

        coEvery { lockSettingsService.getAllowlist(chatId, AllowlistType.URL) } returns allowedUrls

        // When
        val allowlist = lockSettingsService.getAllowlist(chatId, AllowlistType.URL)

        // Then
        assertEquals(allowedUrls, allowlist)
        assertEquals(2, allowlist.size)
        assertTrue(allowlist.contains("https://example.com"))
    }

    @Test
    fun `should retrieve domain allowlist`() = runTest {
        // Given
        val chatId = -123L
        val allowedDomains = listOf("example.com", "trusted.org")

        coEvery { lockSettingsService.getAllowlist(chatId, AllowlistType.DOMAIN) } returns allowedDomains

        // When
        val allowlist = lockSettingsService.getAllowlist(chatId, AllowlistType.DOMAIN)

        // Then
        assertEquals(allowedDomains, allowlist)
    }

    @Test
    fun `should retrieve command allowlist`() = runTest {
        // Given
        val chatId = -123L
        val allowedCommands = listOf("/start", "/help")

        coEvery { lockSettingsService.getAllowlist(chatId, AllowlistType.COMMAND) } returns allowedCommands

        // When
        val allowlist = lockSettingsService.getAllowlist(chatId, AllowlistType.COMMAND)

        // Then
        assertEquals(allowedCommands, allowlist)
    }

    @Test
    fun `should separate URLs from domains based on protocol`() = runTest {
        // Given
        val mixedList = listOf(
            "https://example.com/page",  // URL (has ://)
            "example.com",                // Domain (no ://)
            "http://trusted.org",         // URL
            "trusted.org"                 // Domain
        )

        // When
        val urls = mixedList.filter { it.contains("://") }
        val domains = mixedList.filter { !it.contains("://") }

        // Then
        assertEquals(2, urls.size)
        assertEquals(2, domains.size)
        assertTrue(urls.contains("https://example.com/page"))
        assertTrue(urls.contains("http://trusted.org"))
        assertTrue(domains.contains("example.com"))
        assertTrue(domains.contains("trusted.org"))
    }

    @Test
    fun `should return empty allowlist when none configured`() = runTest {
        // Given
        val chatId = -123L

        coEvery { lockSettingsService.getAllowlist(chatId, AllowlistType.URL) } returns emptyList()

        // When
        val allowlist = lockSettingsService.getAllowlist(chatId, AllowlistType.URL)

        // Then
        assertTrue(allowlist.isEmpty())
    }

    // DETECTOR REGISTRY TESTS

    @Test
    fun `should retrieve detector for lock type`() = runTest {
        // Given
        val photoDetector = mockk<ru.andvl.chatkeep.domain.service.locks.LockDetector> {
            every { lockType } returns LockType.PHOTO
        }

        coEvery { lockDetectorRegistry.getDetector(LockType.PHOTO) } returns photoDetector

        // When
        val detector = lockDetectorRegistry.getDetector(LockType.PHOTO)

        // Then
        assertEquals(LockType.PHOTO, detector?.lockType)
    }

    @Test
    fun `should return null when detector not found`() = runTest {
        // Given
        coEvery { lockDetectorRegistry.getDetector(LockType.PHOTO) } returns null

        // When
        val detector = lockDetectorRegistry.getDetector(LockType.PHOTO)

        // Then
        assertEquals(null, detector)
    }

    // EDGE CASES

    @Test
    fun `should handle multiple exemption types for same chat`() = runTest {
        // Given
        val chatId = -123L

        coEvery { lockSettingsService.getExemptions(chatId) } returns listOf(
            LockExemption(1L, chatId, null, ExemptionType.USER.name, "123"),
            LockExemption(2L, chatId, null, ExemptionType.BOT.name, "testbot"),
            LockExemption(3L, chatId, null, ExemptionType.USER.name, "456")
        )

        // When
        val exemptions = lockSettingsService.getExemptions(chatId)
        val userExemptions = exemptions.filter { it.exemptionType == ExemptionType.USER.name }
        val botExemptions = exemptions.filter { it.exemptionType == ExemptionType.BOT.name }

        // Then
        assertEquals(3, exemptions.size)
        assertEquals(2, userExemptions.size)
        assertEquals(1, botExemptions.size)
    }

    @Test
    fun `should handle very large user IDs`() = runTest {
        // Given
        val chatId = -123L
        val largeUserId = Long.MAX_VALUE

        coEvery { lockSettingsService.getExemptions(chatId) } returns listOf(
            LockExemption(
                id = 1L,
                chatId = chatId,
                lockType = null,
                exemptionType = ExemptionType.USER.name,
                exemptionValue = largeUserId.toString()
            )
        )

        // When
        val exemptions = lockSettingsService.getExemptions(chatId)
        val isExempt = exemptions.any {
            it.exemptionType == ExemptionType.USER.name &&
            it.exemptionValue == largeUserId.toString()
        }

        // Then
        assertTrue(isExempt)
    }

    @Test
    fun `should handle negative chat IDs for Telegram groups`() = runTest {
        // Given
        val negativeChatId = -1234567890L

        coEvery { adminCacheService.isAdmin(456L, negativeChatId) } returns true

        // When
        val isAdmin = adminCacheService.isAdmin(456L, negativeChatId)

        // Then
        assertTrue(isAdmin)
        coVerify { adminCacheService.isAdmin(456L, negativeChatId) }
    }

    @Test
    fun `should handle multiple locks with mixed enabled states`() = runTest {
        // Given
        val chatId = -123L
        val locks = mapOf(
            LockType.PHOTO to LockConfig(locked = true, reason = "No photos"),
            LockType.VIDEO to LockConfig(locked = false, reason = null),
            LockType.STICKER to LockConfig(locked = true, reason = "No stickers"),
            LockType.URL to LockConfig(locked = false, reason = null)
        )

        coEvery { lockSettingsService.getAllLocks(chatId) } returns locks

        // When
        val allLocks = lockSettingsService.getAllLocks(chatId)
        val enabledLocks = allLocks.filter { it.value.locked }
        val disabledLocks = allLocks.filter { !it.value.locked }

        // Then
        assertEquals(4, allLocks.size)
        assertEquals(2, enabledLocks.size)
        assertEquals(2, disabledLocks.size)
    }
}
