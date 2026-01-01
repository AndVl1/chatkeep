package ru.andvl.chatkeep.infrastructure.repository.moderation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.andvl.chatkeep.domain.model.moderation.AdminCacheEntry
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for AdminCacheRepository.
 * Tests upsert, findValidEntry, and delete operations.
 */
@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AdminCacheRepositoryTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("chatkeep_test")
            .withUsername("test")
            .withPassword("test")
    }

    @Autowired
    private lateinit var repository: AdminCacheRepository

    @BeforeEach
    fun cleanup() {
        repository.deleteAll()
    }

    @Test
    fun `upsert should insert new entry`() {
        // Given
        val userId = 12345L
        val chatId = -100123456L
        val now = Instant.now()
        val expiresAt = now.plusSeconds(300)

        // When
        repository.upsert(
            userId = userId,
            chatId = chatId,
            isAdmin = true,
            cachedAt = now,
            expiresAt = expiresAt
        )

        // Then
        val found = repository.findValidEntry(userId, chatId, now)
        assertNotNull(found)
        assertEquals(userId, found.userId)
        assertEquals(chatId, found.chatId)
        assertTrue(found.isAdmin)
    }

    @Test
    fun `upsert should update existing entry`() {
        // Given - insert first entry
        val userId = 12345L
        val chatId = -100123456L
        val now = Instant.now()
        val expiresAt = now.plusSeconds(300)

        repository.upsert(userId, chatId, isAdmin = true, now, expiresAt)

        // When - update with different isAdmin value
        val newCachedAt = now.plusSeconds(60)
        val newExpiresAt = now.plusSeconds(600)
        repository.upsert(userId, chatId, isAdmin = false, newCachedAt, newExpiresAt)

        // Then
        val found = repository.findValidEntry(userId, chatId, now)
        assertNotNull(found)
        assertEquals(false, found.isAdmin)
    }

    @Test
    fun `findValidEntry should return null for expired entry`() {
        // Given - insert entry that expires in the past
        val userId = 12345L
        val chatId = -100123456L
        val now = Instant.now()
        val expiredAt = now.minusSeconds(60)

        repository.upsert(userId, chatId, isAdmin = true, now.minusSeconds(120), expiredAt)

        // When
        val found = repository.findValidEntry(userId, chatId, now)

        // Then
        assertNull(found)
    }

    @Test
    fun `findValidEntry should return entry that has not expired`() {
        // Given
        val userId = 12345L
        val chatId = -100123456L
        val now = Instant.now()
        val expiresAt = now.plusSeconds(300)

        repository.upsert(userId, chatId, isAdmin = true, now, expiresAt)

        // When
        val found = repository.findValidEntry(userId, chatId, now)

        // Then
        assertNotNull(found)
        assertTrue(found.isAdmin)
    }

    @Test
    fun `upsert should handle expired entry correctly`() {
        // Given - insert expired entry
        val userId = 12345L
        val chatId = -100123456L
        val now = Instant.now()
        val expiredAt = now.minusSeconds(60)

        repository.upsert(userId, chatId, isAdmin = true, now.minusSeconds(120), expiredAt)

        // Verify it's expired
        assertNull(repository.findValidEntry(userId, chatId, now))

        // When - upsert with new valid expiration
        val newExpiresAt = now.plusSeconds(300)
        repository.upsert(userId, chatId, isAdmin = false, now, newExpiresAt)

        // Then
        val found = repository.findValidEntry(userId, chatId, now)
        assertNotNull(found)
        assertEquals(false, found.isAdmin)
    }

    @Test
    fun `deleteByUserIdAndChatId should remove entry`() {
        // Given
        val userId = 12345L
        val chatId = -100123456L
        val now = Instant.now()
        val expiresAt = now.plusSeconds(300)

        repository.upsert(userId, chatId, isAdmin = true, now, expiresAt)
        assertNotNull(repository.findValidEntry(userId, chatId, now))

        // When
        repository.deleteByUserIdAndChatId(userId, chatId)

        // Then
        assertNull(repository.findValidEntry(userId, chatId, now))
    }

    @Test
    fun `deleteExpired should remove only expired entries`() {
        // Given
        val now = Instant.now()

        // Valid entry
        repository.upsert(11111L, -100111L, true, now, now.plusSeconds(300))
        // Expired entry
        repository.upsert(22222L, -100222L, true, now.minusSeconds(120), now.minusSeconds(60))

        // Verify both exist
        assertEquals(2, repository.count())

        // When
        repository.deleteExpired(now)

        // Then
        assertEquals(1, repository.count())
        assertNotNull(repository.findValidEntry(11111L, -100111L, now))
    }

    @Test
    fun `multiple users in same chat should have separate entries`() {
        // Given
        val chatId = -100123456L
        val now = Instant.now()
        val expiresAt = now.plusSeconds(300)

        repository.upsert(11111L, chatId, isAdmin = true, now, expiresAt)
        repository.upsert(22222L, chatId, isAdmin = false, now, expiresAt)

        // Then
        val user1 = repository.findValidEntry(11111L, chatId, now)
        val user2 = repository.findValidEntry(22222L, chatId, now)

        assertNotNull(user1)
        assertNotNull(user2)
        assertTrue(user1.isAdmin)
        assertEquals(false, user2.isAdmin)
    }

    @Test
    fun `same user in different chats should have separate entries`() {
        // Given
        val userId = 12345L
        val now = Instant.now()
        val expiresAt = now.plusSeconds(300)

        repository.upsert(userId, -100111L, isAdmin = true, now, expiresAt)
        repository.upsert(userId, -100222L, isAdmin = false, now, expiresAt)

        // Then
        val chat1 = repository.findValidEntry(userId, -100111L, now)
        val chat2 = repository.findValidEntry(userId, -100222L, now)

        assertNotNull(chat1)
        assertNotNull(chat2)
        assertTrue(chat1.isAdmin)
        assertEquals(false, chat2.isAdmin)
    }
}
