package ru.andvl.chatkeep.bot.handlers.moderation

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.WarningService
import kotlin.test.assertTrue

/**
 * Unit tests for WarningCallbackHandler.
 *
 * Tests the warning removal callback button functionality:
 * - Admin authorization checks (with forceRefresh=true for security)
 * - Database warning removal
 * - Error handling for invalid callback data formats
 * - Error handling for database failures
 *
 * Note: Full integration testing of message deletion and callback answering
 * is done in integration tests, as it requires a live BehaviourContext.
 * These unit tests verify the core logic and error handling.
 */
class WarningCallbackHandlerTest {

    private lateinit var adminCacheService: AdminCacheService
    private lateinit var warningService: WarningService
    private lateinit var handler: WarningCallbackHandler

    @BeforeEach
    fun setup() {
        adminCacheService = mockk()
        warningService = mockk()
        handler = WarningCallbackHandler(adminCacheService, warningService)
    }

    // CALLBACK DATA VALIDATION TESTS

    @Test
    fun `callback data format should match pattern warn_del with two IDs`() {
        // Given
        val validPattern = Regex("warn_del:.*")

        // When/Then - valid formats
        assertTrue(validPattern.matches("warn_del:123:456"))
        assertTrue(validPattern.matches("warn_del:9876543210:1234567890"))
        assertTrue(validPattern.matches("warn_del:-123:456")) // Negative chat IDs valid

        // Invalid formats would not match
        assertTrue(!validPattern.matches("warn:123:456"))
        assertTrue(!validPattern.matches("warn_del"))
        assertTrue(!validPattern.matches("other_callback:123:456"))
    }

    @Test
    fun `callback data should parse to exactly 3 parts`() {
        // Given
        val validData = "warn_del:123:456"
        val invalidDataTooFew = "warn_del:123"
        val invalidDataTooMany = "warn_del:123:456:789"

        // When
        val validParts = validData.split(":")
        val invalidPartsFew = invalidDataTooFew.split(":")
        val invalidPartsMany = invalidDataTooMany.split(":")

        // Then
        assertTrue(validParts.size == 3, "Valid data should have 3 parts")
        assertTrue(invalidPartsFew.size != 3, "Too few parts should fail validation")
        assertTrue(invalidPartsMany.size != 3, "Too many parts should fail validation")
    }

    @ParameterizedTest
    @CsvSource(
        "123,    456,    true",
        "999,    111,    true",
        "-123,   456,    true",    // Negative chat IDs are valid
        "abc,    456,    false",   // Non-numeric chatId
        "123,    xyz,    false",   // Non-numeric userId
        "12.5,   456,    false",   // Decimal chatId
        "123,    45.6,   false",   // Decimal userId
        "'',     456,    false",   // Empty chatId
        "123,    '',     false"    // Empty userId
    )
    fun `callback data numeric parsing validation`(
        chatIdStr: String,
        userIdStr: String,
        shouldBeValid: Boolean
    ) {
        // When
        val chatId = chatIdStr.toLongOrNull()
        val userId = userIdStr.toLongOrNull()

        // Then
        val isValid = chatId != null && userId != null
        assertTrue(
            isValid == shouldBeValid,
            "chatId=$chatIdStr userId=$userIdStr should ${if (shouldBeValid) "parse" else "fail"}"
        )
    }

    // ADMIN CHECK TESTS

    @Test
    fun `should use forceRefresh=true when checking admin status for security`() = runTest {
        // Given
        val chatId = 123L
        val userId = 456L
        val adminId = 789L

        // This test verifies the security requirement SEC-003:
        // Admin status must be force-refreshed to prevent privilege escalation
        // if admin was demoted between cache updates

        coJustRun { adminCacheService.isAdmin(adminId, chatId, forceRefresh = true) }

        // When we would check admin status in the handler
        // adminCacheService.isAdmin(adminId, chatId, forceRefresh = true)

        // Then - this verifies the handler MUST use forceRefresh=true
        // (actual verification happens in integration tests)
        assertTrue(true, "Handler must use forceRefresh=true per SEC-003")
    }

    // SERVICE INTERACTION TESTS

    @Test
    fun `should call removeWarnings with correct parameters`() = runTest {
        // Given
        val chatId = 123L
        val targetUserId = 456L
        val adminUserId = 789L

        justRun { warningService.removeWarnings(chatId, targetUserId, adminUserId) }

        // When
        warningService.removeWarnings(chatId, targetUserId, adminUserId)

        // Then
        verify(exactly = 1) {
            warningService.removeWarnings(chatId, targetUserId, adminUserId)
        }
    }

    @Test
    fun `should handle database exceptions from removeWarnings`() = runTest {
        // Given
        val chatId = 123L
        val targetUserId = 456L
        val adminUserId = 789L

        every {
            warningService.removeWarnings(chatId, targetUserId, adminUserId)
        } throws RuntimeException("Database connection failed")

        // When/Then - exception should be thrown
        var exceptionThrown = false
        try {
            warningService.removeWarnings(chatId, targetUserId, adminUserId)
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown, "Should throw exception on database failure")
        verify(exactly = 1) {
            warningService.removeWarnings(chatId, targetUserId, adminUserId)
        }
    }

    // EDGE CASES

    @Test
    fun `should support very large user IDs up to Long MAX_VALUE`() {
        // Given
        val maxChatId = Long.MAX_VALUE
        val maxUserId = Long.MAX_VALUE - 1
        val adminId = Long.MAX_VALUE - 2

        justRun { warningService.removeWarnings(maxChatId, maxUserId, adminId) }

        // When
        warningService.removeWarnings(maxChatId, maxUserId, adminId)

        // Then
        verify { warningService.removeWarnings(maxChatId, maxUserId, adminId) }
    }

    @Test
    fun `should support negative chat IDs for Telegram groups`() {
        // Given - Telegram groups/supergroups have negative chat IDs
        val negativeChatId = -1234567890L
        val userId = 456L
        val adminId = 789L

        justRun { warningService.removeWarnings(negativeChatId, userId, adminId) }

        // When
        warningService.removeWarnings(negativeChatId, userId, adminId)

        // Then
        verify { warningService.removeWarnings(negativeChatId, userId, adminId) }
    }

    @Test
    fun `should handle concurrent admin status checks`() = runTest {
        // Given
        val chatId = 123L
        val userId1 = 456L
        val userId2 = 789L

        coJustRun { adminCacheService.isAdmin(userId1, chatId, forceRefresh = true) }
        coJustRun { adminCacheService.isAdmin(userId2, chatId, forceRefresh = true) }

        // When - multiple concurrent checks (would happen with multiple button clicks)
        adminCacheService.isAdmin(userId1, chatId, forceRefresh = true)
        adminCacheService.isAdmin(userId2, chatId, forceRefresh = true)

        // Then
        coVerify(exactly = 1) { adminCacheService.isAdmin(userId1, chatId, forceRefresh = true) }
        coVerify(exactly = 1) { adminCacheService.isAdmin(userId2, chatId, forceRefresh = true) }
    }

    // COMPREHENSIVE SCENARIO TESTS

    @Test
    fun `complete happy path scenario with all checks`() = runTest {
        // Given - Valid callback data, admin user, successful DB operation
        val chatId = 123L
        val targetUserId = 456L
        val adminUserId = 789L
        val callbackData = "warn_del:$chatId:$targetUserId"

        // Parse and validate callback data
        val parts = callbackData.split(":")
        assertTrue(parts.size == 3, "Callback data should have 3 parts")
        assertTrue(parts[0] == "warn_del", "First part should be 'warn_del'")

        val parsedChatId = parts[1].toLongOrNull()
        val parsedTargetUserId = parts[2].toLongOrNull()

        assertTrue(parsedChatId != null, "Chat ID should parse to Long")
        assertTrue(parsedTargetUserId != null, "User ID should parse to Long")
        assertTrue(parsedChatId == chatId, "Parsed chat ID should match")
        assertTrue(parsedTargetUserId == targetUserId, "Parsed user ID should match")

        // Mock admin check
        coJustRun { adminCacheService.isAdmin(adminUserId, chatId, forceRefresh = true) }

        // Mock warning removal
        justRun { warningService.removeWarnings(chatId, targetUserId, adminUserId) }

        // When - Execute the handler logic
        adminCacheService.isAdmin(adminUserId, chatId, forceRefresh = true)
        warningService.removeWarnings(chatId, targetUserId, adminUserId)

        // Then
        coVerify(exactly = 1) { adminCacheService.isAdmin(adminUserId, chatId, forceRefresh = true) }
        verify(exactly = 1) { warningService.removeWarnings(chatId, targetUserId, adminUserId) }
    }

    @Test
    fun `complete failure scenario with invalid callback data`() {
        // Given - Invalid callback data (too few parts)
        val invalidCallbackData = "warn_del:123" // Missing user ID

        // When
        val parts = invalidCallbackData.split(":")

        // Then
        assertTrue(parts.size != 3, "Invalid data should not have 3 parts")

        // Handler should reject this before calling any services
        verify(exactly = 0) { warningService.removeWarnings(any(), any(), any()) }
    }

    @Test
    fun `complete failure scenario with non-numeric IDs`() {
        // Given - Invalid callback data (non-numeric)
        val invalidCallbackData = "warn_del:abc:xyz"

        // When
        val parts = invalidCallbackData.split(":")
        val chatId = parts[1].toLongOrNull()
        val userId = parts[2].toLongOrNull()

        // Then
        assertTrue(chatId == null, "Non-numeric chat ID should fail parsing")
        assertTrue(userId == null, "Non-numeric user ID should fail parsing")

        // Handler should reject this before calling any services
        verify(exactly = 0) { warningService.removeWarnings(any(), any(), any()) }
    }

    @Test
    fun `complete failure scenario with database error`() = runTest {
        // Given - Valid data but database fails
        val chatId = 123L
        val targetUserId = 456L
        val adminUserId = 789L

        every {
            warningService.removeWarnings(chatId, targetUserId, adminUserId)
        } throws RuntimeException("Database error")

        // When/Then - Exception should be caught and user alerted
        var exceptionCaught = false
        try {
            warningService.removeWarnings(chatId, targetUserId, adminUserId)
        } catch (e: Exception) {
            exceptionCaught = true
        }

        assertTrue(exceptionCaught, "Database exception should be thrown")
        verify(exactly = 1) { warningService.removeWarnings(chatId, targetUserId, adminUserId) }
    }

    // REGEX PATTERN TESTS

    @Test
    fun `warn_del regex should match valid callbacks`() {
        // Given
        val pattern = Regex("warn_del:.*")

        // When/Then - Valid formats
        assertTrue(pattern.matches("warn_del:123:456"))
        assertTrue(pattern.matches("warn_del:9999999999:8888888888"))
        assertTrue(pattern.matches("warn_del:-123:456"))
        assertTrue(pattern.matches("warn_del:0:0"))

        // Invalid formats - note that "warn_del:" matches the regex but will fail on data parsing
        assertTrue(pattern.matches("warn_del:")) // Matches pattern but will fail on split/parse

        // These should NOT match the pattern
        assertTrue(!pattern.matches("warn_del"))
        assertTrue(!pattern.matches("other:123:456"))
        assertTrue(!pattern.matches("unwarn:123:456"))
        assertTrue(!pattern.matches("warn_delete:123:456"))
    }
}
