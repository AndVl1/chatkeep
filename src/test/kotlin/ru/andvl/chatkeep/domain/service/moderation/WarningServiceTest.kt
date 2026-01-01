package ru.andvl.chatkeep.domain.service.moderation

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.model.moderation.Warning
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository
import ru.andvl.chatkeep.infrastructure.repository.moderation.WarningRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

/**
 * Unit tests for WarningService.
 * CRITICAL: Verifies warning expiration logic (soft delete).
 */
class WarningServiceTest {

    private lateinit var warningRepository: WarningRepository
    private lateinit var moderationConfigRepository: ModerationConfigRepository
    private lateinit var punishmentService: PunishmentService
    private lateinit var service: WarningService

    @BeforeEach
    fun setup() {
        warningRepository = mockk()
        moderationConfigRepository = mockk()
        punishmentService = mockk()
        // Mock logAction to do nothing (we just verify it was called)
        justRun { punishmentService.logAction(any(), any(), any(), any(), any(), any(), any(), any()) }
        service = WarningService(warningRepository, moderationConfigRepository, punishmentService)
    }

    // WARNING ISSUANCE TESTS

    @Test
    fun `issueWarning should create warning with correct expiration time`() {
        // Given
        val chatId = 123L
        val userId = 456L
        val issuedById = 789L
        val reason = "Spam"
        val ttlHours = 24

        val config = createConfig(chatId, warningTtlHours = ttlHours)
        every { moderationConfigRepository.findByChatId(chatId) } returns config

        val warningSlot = slot<Warning>()
        every { warningRepository.save(capture(warningSlot)) } answers { firstArg() }

        // When
        val before = Instant.now()
        val result = service.issueWarning(chatId, userId, issuedById, reason)
        val after = Instant.now().plusSeconds(ttlHours.hours.inWholeSeconds)

        // Then
        assertNotNull(result)
        verify { warningRepository.save(any()) }

        val savedWarning = warningSlot.captured
        assertEquals(chatId, savedWarning.chatId)
        assertEquals(userId, savedWarning.userId)
        assertEquals(issuedById, savedWarning.issuedById)
        assertEquals(reason, savedWarning.reason)

        // Verify expiration is approximately 24 hours from now
        assert(savedWarning.expiresAt.isAfter(before.plusSeconds((ttlHours - 1).hours.inWholeSeconds)))
        assert(savedWarning.expiresAt.isBefore(after.plusSeconds(1.hours.inWholeSeconds)))
    }

    @Test
    fun `issueWarning should use default TTL when config not found`() {
        // Given
        val chatId = 123L
        val userId = 456L
        val issuedById = 789L

        every { moderationConfigRepository.findByChatId(chatId) } returns null

        val warningSlot = slot<Warning>()
        every { warningRepository.save(capture(warningSlot)) } answers { firstArg() }

        // When
        service.issueWarning(chatId, userId, issuedById, null)

        // Then
        val savedWarning = warningSlot.captured
        val defaultTtl = 24.hours
        val expectedExpiration = Instant.now().plusSeconds(defaultTtl.inWholeSeconds)

        // Should be close to default TTL (24 hours)
        assert(savedWarning.expiresAt.isAfter(Instant.now().plusSeconds(23.hours.inWholeSeconds)))
        assert(savedWarning.expiresAt.isBefore(expectedExpiration.plusSeconds(1.hours.inWholeSeconds)))
    }

    @Test
    fun `issueWarning should support custom TTL from config`() {
        // Given
        val chatId = 123L
        val customTtl = 72 // 3 days

        val config = createConfig(chatId, warningTtlHours = customTtl)
        every { moderationConfigRepository.findByChatId(chatId) } returns config

        val warningSlot = slot<Warning>()
        every { warningRepository.save(capture(warningSlot)) } answers { firstArg() }

        // When
        service.issueWarning(chatId, 456L, 789L, null)

        // Then
        val savedWarning = warningSlot.captured
        val expectedExpiration = Instant.now().plusSeconds(customTtl.hours.inWholeSeconds)

        assert(savedWarning.expiresAt.isAfter(Instant.now().plusSeconds((customTtl - 1).hours.inWholeSeconds)))
        assert(savedWarning.expiresAt.isBefore(expectedExpiration.plusSeconds(1.hours.inWholeSeconds)))
    }

    // WARNING COUNTING TESTS (CRITICAL: Expiration Logic)

    @Test
    fun `getActiveWarningCount should only count non-expired warnings`() {
        // Given
        val chatId = 123L
        val userId = 456L
        val now = Instant.now()

        // Mock repository to return count of active warnings
        every { warningRepository.countActiveByChatIdAndUserId(chatId, userId, any()) } returns 2

        // When
        val count = service.getActiveWarningCount(chatId, userId)

        // Then
        assertEquals(2, count)
        verify {
            warningRepository.countActiveByChatIdAndUserId(chatId, userId, match {
                // Verify we're passing current time (within 1 second tolerance)
                it.isAfter(now.minusSeconds(1)) && it.isBefore(now.plusSeconds(1))
            })
        }
    }

    @Test
    fun `getActiveWarningCount should return zero when no active warnings`() {
        // Given
        val chatId = 123L
        val userId = 456L

        every { warningRepository.countActiveByChatIdAndUserId(chatId, userId, any()) } returns 0

        // When
        val count = service.getActiveWarningCount(chatId, userId)

        // Then
        assertEquals(0, count)
    }

    @Test
    fun `getActiveWarningCount should pass current timestamp to repository`() {
        // Given
        val chatId = 123L
        val userId = 456L
        val timestampSlot = slot<Instant>()

        every {
            warningRepository.countActiveByChatIdAndUserId(
                chatId,
                userId,
                capture(timestampSlot)
            )
        } returns 1

        // When
        val before = Instant.now()
        service.getActiveWarningCount(chatId, userId)
        val after = Instant.now()

        // Then
        val capturedTime = timestampSlot.captured
        assert(capturedTime.isAfter(before.minusSeconds(1)))
        assert(capturedTime.isBefore(after.plusSeconds(1)))
    }

    // WARNING REMOVAL TESTS

    @Test
    fun `removeWarnings should delete active warnings for user and log unwarn action`() {
        // Given
        val chatId = 123L
        val userId = 456L
        val issuedById = 789L

        justRun { warningRepository.deleteActiveByChatIdAndUserId(any(), any(), any()) }

        // When
        service.removeWarnings(chatId, userId, issuedById)

        // Then
        verify {
            warningRepository.deleteActiveByChatIdAndUserId(chatId, userId, any())
        }
        // Verify that unwarn action was logged via PunishmentService (CODE-002 fix)
        verify {
            punishmentService.logAction(
                chatId = chatId,
                userId = userId,
                issuedById = issuedById,
                actionType = ActionType.UNWARN,
                duration = null,
                reason = null,
                source = PunishmentSource.MANUAL,
                messageText = null
            )
        }
    }

    // THRESHOLD CHECKING TESTS

    @Test
    fun `checkThreshold should return null when warnings below threshold`() {
        // Given
        val chatId = 123L
        val userId = 456L
        val maxWarnings = 3

        val config = createConfig(chatId, maxWarnings = maxWarnings)
        every { moderationConfigRepository.findByChatId(chatId) } returns config
        every { warningRepository.countActiveByChatIdAndUserId(chatId, userId, any()) } returns 2

        // When
        val result = service.checkThreshold(chatId, userId)

        // Then
        assertNull(result, "Should return null when warnings (2) < threshold (3)")
    }

    @Test
    fun `checkThreshold should return action when warnings equal threshold`() {
        // Given
        val chatId = 123L
        val userId = 456L
        val maxWarnings = 3

        val config = createConfig(chatId, maxWarnings = maxWarnings, thresholdAction = "MUTE")
        every { moderationConfigRepository.findByChatId(chatId) } returns config
        every { warningRepository.countActiveByChatIdAndUserId(chatId, userId, any()) } returns 3

        // When
        val result = service.checkThreshold(chatId, userId)

        // Then
        assertNotNull(result)
        assertEquals(PunishmentType.MUTE, result)
    }

    @Test
    fun `checkThreshold should return action when warnings exceed threshold`() {
        // Given
        val chatId = 123L
        val userId = 456L
        val maxWarnings = 3

        val config = createConfig(chatId, maxWarnings = maxWarnings, thresholdAction = "BAN")
        every { moderationConfigRepository.findByChatId(chatId) } returns config
        every { warningRepository.countActiveByChatIdAndUserId(chatId, userId, any()) } returns 5

        // When
        val result = service.checkThreshold(chatId, userId)

        // Then
        assertNotNull(result)
        assertEquals(PunishmentType.BAN, result)
    }

    @Test
    fun `checkThreshold should use default values when config not found`() {
        // Given
        val chatId = 123L
        val userId = 456L

        every { moderationConfigRepository.findByChatId(chatId) } returns null
        every { warningRepository.countActiveByChatIdAndUserId(chatId, userId, any()) } returns 3

        // When
        val result = service.checkThreshold(chatId, userId)

        // Then
        assertNotNull(result, "Default maxWarnings is 3, so 3 warnings should trigger action")
        assertEquals(PunishmentType.MUTE, result, "Default action is MUTE")
    }

    @Test
    fun `checkThreshold should support different punishment types`() {
        // Given
        val chatId = 123L
        val userId = 456L

        val configWarn = createConfig(chatId, maxWarnings = 2, thresholdAction = "WARN")
        val configMute = createConfig(chatId, maxWarnings = 2, thresholdAction = "MUTE")
        val configKick = createConfig(chatId, maxWarnings = 2, thresholdAction = "KICK")
        val configBan = createConfig(chatId, maxWarnings = 2, thresholdAction = "BAN")

        every { warningRepository.countActiveByChatIdAndUserId(chatId, userId, any()) } returns 2

        // When/Then
        every { moderationConfigRepository.findByChatId(chatId) } returns configWarn
        assertEquals(PunishmentType.WARN, service.checkThreshold(chatId, userId))

        every { moderationConfigRepository.findByChatId(chatId) } returns configMute
        assertEquals(PunishmentType.MUTE, service.checkThreshold(chatId, userId))

        every { moderationConfigRepository.findByChatId(chatId) } returns configKick
        assertEquals(PunishmentType.KICK, service.checkThreshold(chatId, userId))

        every { moderationConfigRepository.findByChatId(chatId) } returns configBan
        assertEquals(PunishmentType.BAN, service.checkThreshold(chatId, userId))
    }

    // THRESHOLD DURATION TESTS

    @Test
    fun `getThresholdDurationHours should return config value`() {
        // Given
        val chatId = 123L
        val durationHours = 48

        val config = createConfig(chatId, thresholdDurationHours = durationHours)
        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = service.getThresholdDurationHours(chatId)

        // Then
        assertEquals(durationHours, result)
    }

    @Test
    fun `getThresholdDurationHours should return null when config not found`() {
        // Given
        val chatId = 123L

        every { moderationConfigRepository.findByChatId(chatId) } returns null

        // When
        val result = service.getThresholdDurationHours(chatId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getThresholdDurationHours should return null when duration not set in config`() {
        // Given
        val chatId = 123L

        val config = createConfig(chatId, thresholdDurationHours = null)
        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = service.getThresholdDurationHours(chatId)

        // Then
        assertNull(result)
    }

    // EDGE CASES

    @Test
    fun `issueWarning should handle null reason`() {
        // Given
        val chatId = 123L

        val config = createConfig(chatId)
        every { moderationConfigRepository.findByChatId(chatId) } returns config

        val warningSlot = slot<Warning>()
        every { warningRepository.save(capture(warningSlot)) } answers { firstArg() }

        // When
        service.issueWarning(chatId, 456L, 789L, null)

        // Then
        assertNull(warningSlot.captured.reason)
    }

    @Test
    fun `checkThreshold should only count active warnings not all warnings`() {
        // Given
        val chatId = 123L
        val userId = 456L
        val maxWarnings = 3

        // User has 5 total warnings but only 2 active (3 expired)
        val config = createConfig(chatId, maxWarnings = maxWarnings)
        every { moderationConfigRepository.findByChatId(chatId) } returns config
        every { warningRepository.countActiveByChatIdAndUserId(chatId, userId, any()) } returns 2

        // When
        val result = service.checkThreshold(chatId, userId)

        // Then
        assertNull(result, "Should only count active warnings (2), not expired ones")
    }

    // Helper function
    private fun createConfig(
        chatId: Long,
        maxWarnings: Int = 3,
        warningTtlHours: Int = 24,
        thresholdAction: String = "MUTE",
        thresholdDurationHours: Int? = 24
    ) = ModerationConfig(
        id = null,
        chatId = chatId,
        maxWarnings = maxWarnings,
        warningTtlHours = warningTtlHours,
        thresholdAction = thresholdAction,
        thresholdDurationHours = thresholdDurationHours,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
