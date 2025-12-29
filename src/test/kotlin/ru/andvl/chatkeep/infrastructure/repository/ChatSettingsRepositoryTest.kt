package ru.andvl.chatkeep.infrastructure.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.andvl.chatkeep.domain.model.ChatSettings
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Repository tests for ChatSettingsRepository.
 * Uses real PostgreSQL via Testcontainers for production parity.
 */
@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ChatSettingsRepositoryTest {

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
    private lateinit var repository: ChatSettingsRepository

    @BeforeEach
    fun cleanup() {
        repository.deleteAll()
    }

    @Test
    fun `should save and retrieve chat settings`() {
        // Given
        val settings = createChatSettings(chatId = -100123L, title = "Test Group")

        // When
        val saved = repository.save(settings)

        // Then
        assertNotNull(saved.id, "Should have generated ID")
        assertEquals(-100123L, saved.chatId)
        assertEquals("Test Group", saved.chatTitle)
        assertTrue(saved.collectionEnabled)
    }

    @Test
    fun `findByChatId should return settings when exists`() {
        // Given
        val settings = createChatSettings(chatId = -100456L, title = "Another Group")
        repository.save(settings)

        // When
        val found = repository.findByChatId(-100456L)

        // Then
        assertNotNull(found)
        assertEquals("Another Group", found.chatTitle)
    }

    @Test
    fun `findByChatId should return null when not exists`() {
        // When
        val found = repository.findByChatId(-999999L)

        // Then
        assertNull(found)
    }

    @Test
    fun `existsByChatId should return true when exists`() {
        // Given
        repository.save(createChatSettings(chatId = -100789L))

        // When/Then
        assertTrue(repository.existsByChatId(-100789L))
    }

    @Test
    fun `existsByChatId should return false when not exists`() {
        // When/Then
        assertFalse(repository.existsByChatId(-999999L))
    }

    @Test
    fun `should update existing chat settings`() {
        // Given
        val saved = repository.save(createChatSettings(chatId = -100111L, title = "Original"))

        // When
        val updated = repository.save(
            saved.copy(
                chatTitle = "Updated Title",
                collectionEnabled = false,
                updatedAt = Instant.now()
            )
        )

        // Then
        assertEquals(saved.id, updated.id)
        assertEquals("Updated Title", updated.chatTitle)
        assertFalse(updated.collectionEnabled)
    }

    @Test
    fun `should enforce unique constraint on chat_id`() {
        // Given
        repository.save(createChatSettings(chatId = -100222L))

        // When/Then
        assertThrows<DuplicateKeyException> {
            repository.save(createChatSettings(chatId = -100222L, title = "Duplicate"))
        }
    }

    @Test
    fun `should count all chat settings`() {
        // Given
        repository.save(createChatSettings(chatId = -100001L))
        repository.save(createChatSettings(chatId = -100002L))
        repository.save(createChatSettings(chatId = -100003L))

        // When
        val count = repository.count()

        // Then
        assertEquals(3, count)
    }

    @Test
    fun `should delete chat settings by id`() {
        // Given
        val saved = repository.save(createChatSettings(chatId = -100333L))

        // When
        repository.deleteById(saved.id!!)

        // Then
        assertFalse(repository.existsById(saved.id!!))
        assertNull(repository.findByChatId(-100333L))
    }

    @Test
    fun `should handle null chat title`() {
        // Given
        val settings = createChatSettings(chatId = -100444L, title = null)

        // When
        val saved = repository.save(settings)
        val found = repository.findByChatId(-100444L)

        // Then
        assertNotNull(saved.id)
        assertNull(found?.chatTitle)
    }

    private fun createChatSettings(
        chatId: Long,
        title: String? = "Test Group",
        enabled: Boolean = true
    ) = ChatSettings(
        chatId = chatId,
        chatTitle = title,
        collectionEnabled = enabled,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
