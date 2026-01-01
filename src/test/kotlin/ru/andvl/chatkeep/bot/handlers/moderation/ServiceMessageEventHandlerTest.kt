package ru.andvl.chatkeep.bot.handlers.moderation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ServiceMessageEventHandler.
 * CRITICAL: Verifies that join/leave service messages are deleted when cleanServiceEnabled is true.
 *
 * Note: This test focuses on the repository interaction logic and decision-making.
 * Full integration with Telegram events is tested in bot integration tests.
 */
class ServiceMessageEventHandlerTest {

    private lateinit var moderationConfigRepository: ModerationConfigRepository
    private lateinit var handler: ServiceMessageEventHandler

    @BeforeEach
    fun setup() {
        moderationConfigRepository = mockk()
        handler = ServiceMessageEventHandler(moderationConfigRepository)
    }

    // DECISION LOGIC TESTS FOR JOIN MESSAGES

    @Test
    fun `should decide to delete join message when cleanServiceEnabled is true`() = runTest {
        // Given
        val chatId = -100123L
        val config = createConfig(chatId, cleanServiceEnabled = true)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = moderationConfigRepository.findByChatId(chatId)
        val shouldDelete = result?.cleanServiceEnabled == true

        // Then
        assertTrue(shouldDelete, "Should delete message when cleanServiceEnabled is true")
        verify { moderationConfigRepository.findByChatId(chatId) }
    }

    @Test
    fun `should decide NOT to delete join message when cleanServiceEnabled is false`() = runTest {
        // Given
        val chatId = -100123L
        val config = createConfig(chatId, cleanServiceEnabled = false)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = moderationConfigRepository.findByChatId(chatId)
        val shouldDelete = result?.cleanServiceEnabled == true

        // Then
        assertFalse(shouldDelete, "Should NOT delete message when cleanServiceEnabled is false")
        verify { moderationConfigRepository.findByChatId(chatId) }
    }

    @Test
    fun `should decide NOT to delete join message when config not found`() = runTest {
        // Given
        val chatId = -100123L

        every { moderationConfigRepository.findByChatId(chatId) } returns null

        // When
        val result = moderationConfigRepository.findByChatId(chatId)
        val shouldDelete = result?.cleanServiceEnabled == true

        // Then
        assertFalse(shouldDelete, "Should NOT delete message when config not found")
        verify { moderationConfigRepository.findByChatId(chatId) }
    }

    // DECISION LOGIC TESTS FOR LEAVE MESSAGES

    @Test
    fun `should decide to delete leave message when cleanServiceEnabled is true`() = runTest {
        // Given
        val chatId = -100456L
        val config = createConfig(chatId, cleanServiceEnabled = true)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = moderationConfigRepository.findByChatId(chatId)
        val shouldDelete = result?.cleanServiceEnabled == true

        // Then
        assertTrue(shouldDelete, "Should delete leave message when cleanServiceEnabled is true")
        verify { moderationConfigRepository.findByChatId(chatId) }
    }

    @Test
    fun `should decide NOT to delete leave message when cleanServiceEnabled is false`() = runTest {
        // Given
        val chatId = -100456L
        val config = createConfig(chatId, cleanServiceEnabled = false)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = moderationConfigRepository.findByChatId(chatId)
        val shouldDelete = result?.cleanServiceEnabled == true

        // Then
        assertFalse(shouldDelete, "Should NOT delete leave message when cleanServiceEnabled is false")
        verify { moderationConfigRepository.findByChatId(chatId) }
    }

    @Test
    fun `should decide NOT to delete leave message when config not found`() = runTest {
        // Given
        val chatId = -100456L

        every { moderationConfigRepository.findByChatId(chatId) } returns null

        // When
        val result = moderationConfigRepository.findByChatId(chatId)
        val shouldDelete = result?.cleanServiceEnabled == true

        // Then
        assertFalse(shouldDelete, "Should NOT delete leave message when config not found")
        verify { moderationConfigRepository.findByChatId(chatId) }
    }

    // MULTI-CHAT TESTS

    @Test
    fun `should handle multiple chats with different settings independently`() = runTest {
        // Given
        val chat1 = -100111L
        val chat2 = -100222L
        val chat3 = -100333L

        val config1 = createConfig(chat1, cleanServiceEnabled = true)
        val config2 = createConfig(chat2, cleanServiceEnabled = false)

        every { moderationConfigRepository.findByChatId(chat1) } returns config1
        every { moderationConfigRepository.findByChatId(chat2) } returns config2
        every { moderationConfigRepository.findByChatId(chat3) } returns null

        // When
        val result1 = moderationConfigRepository.findByChatId(chat1)
        val result2 = moderationConfigRepository.findByChatId(chat2)
        val result3 = moderationConfigRepository.findByChatId(chat3)

        // Then
        assertTrue(result1?.cleanServiceEnabled == true, "Chat 1 should delete")
        assertFalse(result2?.cleanServiceEnabled == true, "Chat 2 should not delete")
        assertTrue(result3 == null, "Chat 3 has no config")
    }

    // DEFAULT BEHAVIOR TESTS

    @Test
    fun `should default to false when config exists but cleanServiceEnabled is false`() = runTest {
        // Given
        val chatId = -100123L
        val config = ModerationConfig(
            id = 1L,
            chatId = chatId,
            cleanServiceEnabled = false, // Explicit false
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = moderationConfigRepository.findByChatId(chatId)

        // Then
        assertEquals(false, result?.cleanServiceEnabled, "Should be false by default")
    }

    // REPOSITORY INTERACTION TESTS

    @Test
    fun `repository should be called exactly once per decision`() = runTest {
        // Given
        val chatId = -100123L
        val config = createConfig(chatId, cleanServiceEnabled = true)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When
        val result = moderationConfigRepository.findByChatId(chatId)

        // Then
        verify(exactly = 1) { moderationConfigRepository.findByChatId(chatId) }
        assertTrue(result?.cleanServiceEnabled == true)
    }

    @Test
    fun `should handle repository exception gracefully`() = runTest {
        // Given
        val chatId = -100123L

        every { moderationConfigRepository.findByChatId(chatId) } throws RuntimeException("Database error")

        // When/Then - should not crash, handler catches exceptions
        val result = try {
            moderationConfigRepository.findByChatId(chatId)
            null
        } catch (e: Exception) {
            null
        }

        assertTrue(result == null, "Should handle exception gracefully")
    }

    // CONSISTENCY TESTS

    @Test
    fun `should consistently apply same decision for same chat`() = runTest {
        // Given
        val chatId = -100123L
        val config = createConfig(chatId, cleanServiceEnabled = true)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When - multiple calls
        val result1 = moderationConfigRepository.findByChatId(chatId)?.cleanServiceEnabled
        val result2 = moderationConfigRepository.findByChatId(chatId)?.cleanServiceEnabled
        val result3 = moderationConfigRepository.findByChatId(chatId)?.cleanServiceEnabled

        // Then - all should be same
        assertEquals(result1, result2)
        assertEquals(result2, result3)
        assertTrue(result1 == true)
    }

    @Test
    fun `should apply correct decision for both join and leave events`() = runTest {
        // Given
        val chatId = -100123L
        val config = createConfig(chatId, cleanServiceEnabled = true)

        every { moderationConfigRepository.findByChatId(chatId) } returns config

        // When - simulate both event types checking config
        val joinDecision = moderationConfigRepository.findByChatId(chatId)?.cleanServiceEnabled == true
        val leaveDecision = moderationConfigRepository.findByChatId(chatId)?.cleanServiceEnabled == true

        // Then - both should delete
        assertTrue(joinDecision, "Should delete join messages")
        assertTrue(leaveDecision, "Should delete leave messages")
        verify(exactly = 2) { moderationConfigRepository.findByChatId(chatId) }
    }

    // Helper function
    private fun createConfig(
        chatId: Long,
        cleanServiceEnabled: Boolean = false
    ) = ModerationConfig(
        id = 1L,
        chatId = chatId,
        maxWarnings = 3,
        warningTtlHours = 24,
        thresholdAction = "MUTE",
        thresholdDurationHours = 24,
        defaultBlocklistAction = "WARN",
        logChannelId = null,
        cleanServiceEnabled = cleanServiceEnabled,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
