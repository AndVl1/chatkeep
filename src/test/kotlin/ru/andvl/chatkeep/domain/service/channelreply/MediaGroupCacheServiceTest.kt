package ru.andvl.chatkeep.domain.service.channelreply

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for MediaGroupCacheService.
 *
 * Tests the TTL-based caching mechanism for media group deduplication.
 * CRITICAL: Verifies TTL expiration logic to prevent memory leaks.
 */
class MediaGroupCacheServiceTest {

    private lateinit var service: MediaGroupCacheService

    @BeforeEach
    fun setup() {
        service = MediaGroupCacheService()
    }

    @Nested
    inner class BasicCachingTests {

        @Test
        fun `isProcessed with new ID returns false`() {
            // Given
            val mediaGroupId = "group_123"

            // When
            val result = service.isProcessed(mediaGroupId)

            // Then
            assertFalse(result, "New media group ID should not be marked as processed")
        }

        @Test
        fun `markProcessed then isProcessed returns true`() {
            // Given
            val mediaGroupId = "group_123"

            // When
            service.markProcessed(mediaGroupId)
            val result = service.isProcessed(mediaGroupId)

            // Then
            assertTrue(result, "Media group ID should be marked as processed after markProcessed")
        }

        @Test
        fun `isProcessed with different IDs are independent`() {
            // Given
            val id1 = "group_1"
            val id2 = "group_2"

            // When
            service.markProcessed(id1)

            // Then
            assertTrue(service.isProcessed(id1), "ID 1 should be processed")
            assertFalse(service.isProcessed(id2), "ID 2 should not be processed")
        }

        @Test
        fun `markProcessed can be called multiple times for same ID`() {
            // Given
            val mediaGroupId = "group_123"

            // When
            service.markProcessed(mediaGroupId)
            service.markProcessed(mediaGroupId) // Second call
            service.markProcessed(mediaGroupId) // Third call

            // Then
            assertTrue(service.isProcessed(mediaGroupId), "Should still be processed")
        }
    }

    @Nested
    inner class TTLExpirationTests {

        @Test
        fun `isProcessed after TTL returns false`() = runBlocking {
            // Given
            val mediaGroupId = "group_expire"
            val ttlMinutes = 5L
            val ttlSeconds = ttlMinutes * 60

            // Mark as processed
            service.markProcessed(mediaGroupId)

            // Verify it's processed
            assertTrue(service.isProcessed(mediaGroupId))

            // Simulate TTL expiration by using reflection to modify the timestamp
            // (In real scenario, we'd wait 5 minutes, but we simulate it)
            val cache = service::class.java.getDeclaredField("cache").apply {
                isAccessible = true
            }.get(service) as java.util.concurrent.ConcurrentHashMap<String, Instant>

            // Set timestamp to 6 minutes ago (past TTL)
            cache[mediaGroupId] = Instant.now().minusSeconds(ttlSeconds + 60)

            // When - call isProcessed which triggers cleanExpired
            val result = service.isProcessed(mediaGroupId)

            // Then
            assertFalse(result, "Expired media group ID should return false after TTL")
        }

        @Test
        fun `isProcessed within TTL returns true`() = runBlocking {
            // Given
            val mediaGroupId = "group_valid"

            // Mark as processed
            service.markProcessed(mediaGroupId)

            // Simulate 1 minute passed (within 5 minute TTL)
            val cache = service::class.java.getDeclaredField("cache").apply {
                isAccessible = true
            }.get(service) as java.util.concurrent.ConcurrentHashMap<String, Instant>

            cache[mediaGroupId] = Instant.now().minusSeconds(60) // 1 minute ago

            // When
            val result = service.isProcessed(mediaGroupId)

            // Then
            assertTrue(result, "Media group ID should still be valid within TTL")
        }

        @Test
        fun `cleanExpired removes only expired entries`() = runBlocking {
            // Given
            val expiredId = "group_expired"
            val validId = "group_valid"

            // Mark both as processed
            service.markProcessed(expiredId)
            service.markProcessed(validId)

            // Access cache via reflection
            val cache = service::class.java.getDeclaredField("cache").apply {
                isAccessible = true
            }.get(service) as java.util.concurrent.ConcurrentHashMap<String, Instant>

            // Set one to expired, one to valid
            cache[expiredId] = Instant.now().minusSeconds(6 * 60) // 6 minutes ago (expired)
            cache[validId] = Instant.now().minusSeconds(60)       // 1 minute ago (valid)

            // When - call isProcessed which triggers cleanExpired
            val expiredResult = service.isProcessed(expiredId)
            val validResult = service.isProcessed(validId)

            // Then
            assertFalse(expiredResult, "Expired ID should be removed")
            assertTrue(validResult, "Valid ID should remain")
        }

        @Test
        fun `multiple expired entries are all cleaned`() = runBlocking {
            // Given
            val expired1 = "group_exp_1"
            val expired2 = "group_exp_2"
            val expired3 = "group_exp_3"
            val valid = "group_valid"

            // Mark all as processed
            service.markProcessed(expired1)
            service.markProcessed(expired2)
            service.markProcessed(expired3)
            service.markProcessed(valid)

            // Access cache
            val cache = service::class.java.getDeclaredField("cache").apply {
                isAccessible = true
            }.get(service) as java.util.concurrent.ConcurrentHashMap<String, Instant>

            // Set three to expired
            val expiredTime = Instant.now().minusSeconds(6 * 60)
            cache[expired1] = expiredTime
            cache[expired2] = expiredTime
            cache[expired3] = expiredTime
            cache[valid] = Instant.now() // Just now (valid)

            // When - trigger clean by checking any ID
            service.isProcessed("any_id")

            // Then - check remaining entries
            assertFalse(service.isProcessed(expired1))
            assertFalse(service.isProcessed(expired2))
            assertFalse(service.isProcessed(expired3))
            assertTrue(service.isProcessed(valid))
        }
    }

    @Nested
    inner class EdgeCaseTests {

        @Test
        fun `empty string ID is handled correctly`() {
            // Given
            val mediaGroupId = ""

            // When
            service.markProcessed(mediaGroupId)
            val result = service.isProcessed(mediaGroupId)

            // Then
            assertTrue(result)
        }

        @Test
        fun `very long ID is handled correctly`() {
            // Given
            val mediaGroupId = "a".repeat(1000)

            // When
            service.markProcessed(mediaGroupId)
            val result = service.isProcessed(mediaGroupId)

            // Then
            assertTrue(result)
        }

        @Test
        fun `special characters in ID are handled correctly`() {
            // Given
            val mediaGroupId = "group_!@#$%^&*()_+-={}[]|:;<>?,./"

            // When
            service.markProcessed(mediaGroupId)
            val result = service.isProcessed(mediaGroupId)

            // Then
            assertTrue(result)
        }

        @Test
        fun `unicode characters in ID are handled correctly`() {
            // Given
            val mediaGroupId = "группа_123_🔥"

            // When
            service.markProcessed(mediaGroupId)
            val result = service.isProcessed(mediaGroupId)

            // Then
            assertTrue(result)
        }

        @Test
        fun `concurrent access to same ID`() = runBlocking {
            // Given
            val mediaGroupId = "concurrent_test"

            // When - mark and check concurrently
            val jobs = List(10) {
                this@runBlocking.launch {
                    service.markProcessed(mediaGroupId)
                    service.isProcessed(mediaGroupId)
                }
            }

            jobs.forEach { it.join() }

            // Then - should be processed
            assertTrue(service.isProcessed(mediaGroupId))
        }

        @Test
        fun `markProcessed updates timestamp on duplicate calls`() = runBlocking {
            // Given
            val mediaGroupId = "update_test"

            // Mark initially
            service.markProcessed(mediaGroupId)

            // Access cache
            val cache = service::class.java.getDeclaredField("cache").apply {
                isAccessible = true
            }.get(service) as java.util.concurrent.ConcurrentHashMap<String, Instant>

            val firstTimestamp = cache[mediaGroupId]

            // Wait a bit
            delay(10)

            // When - mark again
            service.markProcessed(mediaGroupId)
            val secondTimestamp = cache[mediaGroupId]

            // Then - timestamp should be updated
            assertTrue(
                secondTimestamp!!.isAfter(firstTimestamp),
                "Second timestamp should be after first"
            )
        }
    }

    @Nested
    inner class RealWorldScenarioTests {

        @Test
        fun `typical media group deduplication flow`() = runBlocking {
            // Scenario: Telegram sends 3 photos in a media group

            // Given
            val mediaGroupId = "AgADBAADGKoxG1234567890"

            // When - first photo arrives
            val isFirstProcessed = service.isProcessed(mediaGroupId)
            assertFalse(isFirstProcessed, "First photo should not be processed yet")

            service.markProcessed(mediaGroupId)

            // When - second photo arrives (duplicate)
            val isSecondProcessed = service.isProcessed(mediaGroupId)
            assertTrue(isSecondProcessed, "Second photo should be marked as duplicate")

            // When - third photo arrives (duplicate)
            val isThirdProcessed = service.isProcessed(mediaGroupId)
            assertTrue(isThirdProcessed, "Third photo should be marked as duplicate")
        }

        @Test
        fun `different media groups are independent`() {
            // Scenario: Two different media groups arrive simultaneously

            // Given
            val group1 = "AgADBAADGKoxG111111111"
            val group2 = "AgADBAADGKoxG222222222"

            // When
            service.markProcessed(group1)

            // Then
            assertTrue(service.isProcessed(group1))
            assertFalse(service.isProcessed(group2), "Different group should be independent")

            // When
            service.markProcessed(group2)

            // Then
            assertTrue(service.isProcessed(group1))
            assertTrue(service.isProcessed(group2))
        }

        @Test
        fun `same media group can be reused after TTL expiration`() = runBlocking {
            // Scenario: Same media group ID appears again after 5+ minutes
            // (This is unlikely but theoretically possible)

            // Given
            val mediaGroupId = "reused_group"

            // First occurrence
            service.markProcessed(mediaGroupId)
            assertTrue(service.isProcessed(mediaGroupId))

            // Simulate TTL expiration
            val cache = service::class.java.getDeclaredField("cache").apply {
                isAccessible = true
            }.get(service) as java.util.concurrent.ConcurrentHashMap<String, Instant>

            cache[mediaGroupId] = Instant.now().minusSeconds(6 * 60)

            // When - trigger clean
            val isExpired = service.isProcessed(mediaGroupId)
            assertFalse(isExpired, "Should be expired")

            // Second occurrence (after TTL)
            service.markProcessed(mediaGroupId)
            val isProcessedAgain = service.isProcessed(mediaGroupId)

            // Then
            assertTrue(isProcessedAgain, "Should be processed again after TTL reset")
        }
    }

    @Nested
    inner class MemoryLeakPreventionTests {

        @Test
        fun `cache does not grow indefinitely`() = runBlocking {
            // Given - process many media groups over time
            val groupCount = 100

            // Access cache
            val cache = service::class.java.getDeclaredField("cache").apply {
                isAccessible = true
            }.get(service) as java.util.concurrent.ConcurrentHashMap<String, Instant>

            // When - add many groups
            for (i in 1..groupCount) {
                service.markProcessed("group_$i")
            }

            // All should be in cache
            assertEquals(groupCount, cache.size)

            // Expire half of them
            for (i in 1..groupCount / 2) {
                cache["group_$i"] = Instant.now().minusSeconds(6 * 60)
            }

            // Trigger cleanup
            service.isProcessed("trigger_cleanup")

            // Then - cache should be cleaned
            assertTrue(
                cache.size < groupCount,
                "Cache size should be reduced after cleanup"
            )
        }

        @Test
        fun `cleanExpired is called on every isProcessed`() = runBlocking {
            // This test verifies that cleanExpired runs automatically,
            // preventing memory leaks even without explicit calls

            // Given
            val old1 = "old_1"
            val old2 = "old_2"

            service.markProcessed(old1)
            service.markProcessed(old2)

            // Access cache
            val cache = service::class.java.getDeclaredField("cache").apply {
                isAccessible = true
            }.get(service) as java.util.concurrent.ConcurrentHashMap<String, Instant>

            // Expire both
            cache[old1] = Instant.now().minusSeconds(6 * 60)
            cache[old2] = Instant.now().minusSeconds(6 * 60)

            // When - call isProcessed for any ID (even non-existent)
            service.isProcessed("any_new_id")

            // Then - expired entries should be removed
            assertFalse(cache.containsKey(old1))
            assertFalse(cache.containsKey(old2))
        }
    }

    // Helper to assert cache size via reflection
    private fun assertEquals(expected: Int, actual: Int) {
        kotlin.test.assertEquals(expected, actual)
    }
}
