package ru.andvl.chatkeep

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.andvl.chatkeep.domain.model.ChatSettings
import ru.andvl.chatkeep.infrastructure.repository.ChatSettingsRepository
import ru.andvl.chatkeep.infrastructure.repository.MessageRepository
import java.time.Instant
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Application integration tests.
 * Verifies the application starts correctly and can interact with the database.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ChatkeepApplicationTests {

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
    private lateinit var flyway: Flyway

    @Autowired
    private lateinit var chatSettingsRepository: ChatSettingsRepository

    @Autowired
    private lateinit var messageRepository: MessageRepository

    @Test
    fun `context loads with real PostgreSQL`() {
        // If we get here, context loaded successfully
        assertTrue(postgres.isRunning, "PostgreSQL container should be running")
    }

    @Test
    fun `flyway migrations are applied on startup`() {
        val info = flyway.info()
        val applied = info.applied()

        assertTrue(applied.isNotEmpty(), "Should have applied migrations")
        assertTrue(applied.all { it.state.isApplied }, "All migrations should be applied")
    }

    @Test
    fun `repositories are autowired correctly`() {
        assertNotNull(chatSettingsRepository)
        assertNotNull(messageRepository)
    }

    @Test
    fun `database operations work through repositories`() {
        // This verifies the full stack: Spring -> Repository -> JDBC -> PostgreSQL
        val count = chatSettingsRepository.count()
        // Just checking we can query without exceptions
        assertTrue(count >= 0)
    }

    @Test
    fun `can perform CRUD operations on ChatSettings`() {
        // Clean up first
        val existingSettings = chatSettingsRepository.findByChatId(-999888777L)
        if (existingSettings != null) {
            chatSettingsRepository.deleteById(existingSettings.id!!)
        }

        // Create
        val settings = ChatSettings(
            chatId = -999888777L,
            chatTitle = "Integration Test Chat",
            collectionEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val saved = chatSettingsRepository.save(settings)
        assertNotNull(saved.id, "Should have generated ID")

        // Read
        val found = chatSettingsRepository.findByChatId(-999888777L)
        assertNotNull(found, "Should find saved settings")

        // Update
        val updated = chatSettingsRepository.save(found.copy(chatTitle = "Updated Title"))
        assertTrue(updated.chatTitle == "Updated Title", "Should update title")

        // Delete
        chatSettingsRepository.deleteById(updated.id!!)
        val deleted = chatSettingsRepository.findByChatId(-999888777L)
        assertTrue(deleted == null, "Should be deleted")
    }

    @Test
    fun `message repository count queries work`() {
        // Test that custom @Query methods work with real PostgreSQL
        val count = messageRepository.countByChatId(-999888777L)
        assertTrue(count >= 0, "Count should be non-negative")

        val uniqueUsers = messageRepository.countUniqueUsersByChatId(-999888777L)
        assertTrue(uniqueUsers >= 0, "Unique users count should be non-negative")
    }
}
