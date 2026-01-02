package ru.andvl.chatkeep.infrastructure.repository.moderation

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.andvl.chatkeep.config.TestConfiguration
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for ModerationConfigRepository.
 * CRITICAL: Verifies clean_service_enabled column and updateCleanServiceEnabled method.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration::class)
@Transactional
class ModerationConfigRepositoryTest {

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
    private lateinit var repository: ModerationConfigRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    // SCHEMA VALIDATION TESTS

    @Test
    fun `moderation_config table should have clean_service_enabled column`() {
        // When
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type, is_nullable, column_default
            FROM information_schema.columns
            WHERE table_name = 'moderation_config' AND column_name = 'clean_service_enabled'
            """.trimIndent()
        )

        // Then
        assertTrue(columns.isNotEmpty(), "clean_service_enabled column should exist")

        val column = columns.first()
        assertEquals("clean_service_enabled", column["column_name"])
        assertEquals("boolean", column["data_type"])
        assertEquals("NO", column["is_nullable"], "Column should be NOT NULL")
        assertTrue(column["column_default"].toString().contains("false"), "Default should be false")
    }

    // CREATE TESTS

    @Test
    fun `should create moderation config with cleanServiceEnabled false by default`() {
        // Given
        val chatId = -100123L
        val config = ModerationConfig(chatId = chatId)

        // When
        val saved = repository.save(config)

        // Then
        assertNotNull(saved.id)
        assertEquals(chatId, saved.chatId)
        assertEquals(false, saved.cleanServiceEnabled, "Should default to false")
    }

    @Test
    fun `should create moderation config with cleanServiceEnabled true`() {
        // Given
        val chatId = -100456L
        val config = ModerationConfig(chatId = chatId, cleanServiceEnabled = true)

        // When
        val saved = repository.save(config)

        // Then
        assertNotNull(saved.id)
        assertEquals(chatId, saved.chatId)
        assertEquals(true, saved.cleanServiceEnabled)
    }

    @Test
    fun `should create moderation config with cleanServiceEnabled false explicitly`() {
        // Given
        val chatId = -100789L
        val config = ModerationConfig(chatId = chatId, cleanServiceEnabled = false)

        // When
        val saved = repository.save(config)

        // Then
        assertNotNull(saved.id)
        assertEquals(chatId, saved.chatId)
        assertEquals(false, saved.cleanServiceEnabled)
    }

    // READ TESTS

    @Test
    fun `findByChatId should return config with cleanServiceEnabled`() {
        // Given
        val chatId = -100111L
        val config = ModerationConfig(chatId = chatId, cleanServiceEnabled = true)
        repository.save(config)

        // When
        val found = repository.findByChatId(chatId)

        // Then
        assertNotNull(found)
        assertEquals(chatId, found.chatId)
        assertEquals(true, found.cleanServiceEnabled)
    }

    @Test
    fun `findByChatId should return null when config not found`() {
        // Given
        val chatId = -999999L

        // When
        val found = repository.findByChatId(chatId)

        // Then
        assertTrue(found == null, "Should return null for non-existent chat")
    }

    // UPDATE TESTS

    @Test
    fun `updateCleanServiceEnabled should enable clean service`() {
        // Given
        val chatId = -100222L
        val config = ModerationConfig(chatId = chatId, cleanServiceEnabled = false)
        repository.save(config)

        // When
        val updated = repository.updateCleanServiceEnabled(chatId, true)

        // Then
        assertEquals(1, updated, "Should update 1 row")

        val found = repository.findByChatId(chatId)
        assertNotNull(found)
        assertEquals(true, found.cleanServiceEnabled)
    }

    @Test
    fun `updateCleanServiceEnabled should disable clean service`() {
        // Given
        val chatId = -100333L
        val config = ModerationConfig(chatId = chatId, cleanServiceEnabled = true)
        repository.save(config)

        // When
        val updated = repository.updateCleanServiceEnabled(chatId, false)

        // Then
        assertEquals(1, updated, "Should update 1 row")

        val found = repository.findByChatId(chatId)
        assertNotNull(found)
        assertEquals(false, found.cleanServiceEnabled)
    }

    @Test
    fun `updateCleanServiceEnabled should return 0 when chat not found`() {
        // Given
        val chatId = -999999L

        // When
        val updated = repository.updateCleanServiceEnabled(chatId, true)

        // Then
        assertEquals(0, updated, "Should return 0 when chat not found")
    }

    @Test
    fun `updateCleanServiceEnabled should update updated_at timestamp`() {
        // Given
        val chatId = -100444L
        val config = ModerationConfig(chatId = chatId, cleanServiceEnabled = false)
        val saved = repository.save(config)
        val originalUpdatedAt = saved.updatedAt

        // Wait a tiny bit to ensure timestamp difference
        Thread.sleep(10)

        // When
        repository.updateCleanServiceEnabled(chatId, true)

        // Then
        val found = repository.findByChatId(chatId)
        assertNotNull(found)
        assertTrue(
            found.updatedAt.isAfter(originalUpdatedAt),
            "updated_at should be updated"
        )
    }

    @Test
    fun `updateCleanServiceEnabled should not affect other fields`() {
        // Given
        val chatId = -100555L
        val config = ModerationConfig(
            chatId = chatId,
            maxWarnings = 5,
            warningTtlHours = 48,
            thresholdAction = "BAN",
            thresholdDurationHours = 72,
            defaultBlocklistAction = "DELETE",
            logChannelId = -987654L,
            cleanServiceEnabled = false
        )
        repository.save(config)

        // When
        repository.updateCleanServiceEnabled(chatId, true)

        // Then
        val found = repository.findByChatId(chatId)
        assertNotNull(found)
        assertEquals(true, found.cleanServiceEnabled)
        assertEquals(5, found.maxWarnings)
        assertEquals(48, found.warningTtlHours)
        assertEquals("BAN", found.thresholdAction)
        assertEquals(72, found.thresholdDurationHours)
        assertEquals("DELETE", found.defaultBlocklistAction)
        assertEquals(-987654L, found.logChannelId)
    }

    // TOGGLE TESTS

    @Test
    fun `should toggle cleanServiceEnabled multiple times`() {
        // Given
        val chatId = -100666L
        val config = ModerationConfig(chatId = chatId, cleanServiceEnabled = false)
        repository.save(config)

        // When/Then - toggle on
        repository.updateCleanServiceEnabled(chatId, true)
        var found = repository.findByChatId(chatId)
        assertEquals(true, found?.cleanServiceEnabled)

        // Toggle off
        repository.updateCleanServiceEnabled(chatId, false)
        found = repository.findByChatId(chatId)
        assertEquals(false, found?.cleanServiceEnabled)

        // Toggle on again
        repository.updateCleanServiceEnabled(chatId, true)
        found = repository.findByChatId(chatId)
        assertEquals(true, found?.cleanServiceEnabled)
    }

    @Test
    fun `should handle idempotent updates`() {
        // Given
        val chatId = -100777L
        val config = ModerationConfig(chatId = chatId, cleanServiceEnabled = false)
        repository.save(config)

        // When - set to true twice
        val updated1 = repository.updateCleanServiceEnabled(chatId, true)
        val updated2 = repository.updateCleanServiceEnabled(chatId, true)

        // Then - both should succeed
        assertEquals(1, updated1)
        assertEquals(1, updated2)

        val found = repository.findByChatId(chatId)
        assertEquals(true, found?.cleanServiceEnabled)
    }

    // MULTIPLE CHATS TESTS

    @Test
    fun `should manage cleanServiceEnabled independently for multiple chats`() {
        // Given
        val chat1 = -100001L
        val chat2 = -100002L
        val chat3 = -100003L

        repository.save(ModerationConfig(chatId = chat1, cleanServiceEnabled = false))
        repository.save(ModerationConfig(chatId = chat2, cleanServiceEnabled = true))
        repository.save(ModerationConfig(chatId = chat3, cleanServiceEnabled = false))

        // When
        repository.updateCleanServiceEnabled(chat1, true)
        // chat2 stays true
        repository.updateCleanServiceEnabled(chat3, false)

        // Then
        assertEquals(true, repository.findByChatId(chat1)?.cleanServiceEnabled)
        assertEquals(true, repository.findByChatId(chat2)?.cleanServiceEnabled)
        assertEquals(false, repository.findByChatId(chat3)?.cleanServiceEnabled)
    }

    @Test
    fun `should not affect other chats when updating one`() {
        // Given
        val chat1 = -100011L
        val chat2 = -100022L

        repository.save(ModerationConfig(chatId = chat1, cleanServiceEnabled = false))
        repository.save(ModerationConfig(chatId = chat2, cleanServiceEnabled = false))

        // When
        repository.updateCleanServiceEnabled(chat1, true)

        // Then
        assertEquals(true, repository.findByChatId(chat1)?.cleanServiceEnabled)
        assertEquals(false, repository.findByChatId(chat2)?.cleanServiceEnabled, "Other chat should not be affected")
    }

    // EDGE CASES

    @Test
    fun `should handle very large negative chat IDs`() {
        // Given
        val chatId = -1001234567890L // Telegram supergroup format
        val config = ModerationConfig(chatId = chatId, cleanServiceEnabled = true)

        // When
        val saved = repository.save(config)
        val updated = repository.updateCleanServiceEnabled(chatId, false)
        val found = repository.findByChatId(chatId)

        // Then
        assertNotNull(saved.id)
        assertEquals(1, updated)
        assertNotNull(found)
        assertEquals(false, found.cleanServiceEnabled)
    }

    @Test
    fun `should work with all other fields at default values`() {
        // Given
        val chatId = -100888L
        val config = ModerationConfig(chatId = chatId) // All defaults

        // When
        repository.save(config)
        repository.updateCleanServiceEnabled(chatId, true)
        val found = repository.findByChatId(chatId)

        // Then
        assertNotNull(found)
        assertEquals(true, found.cleanServiceEnabled)
        assertEquals(3, found.maxWarnings) // Default
        assertEquals(24, found.warningTtlHours) // Default
    }

    // CONSISTENCY TESTS

    @Test
    fun `saved entity should match retrieved entity`() {
        // Given
        val chatId = -100999L
        val config = ModerationConfig(
            chatId = chatId,
            cleanServiceEnabled = true
        )

        // When
        val saved = repository.save(config)
        val found = repository.findByChatId(chatId)

        // Then
        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertEquals(saved.chatId, found.chatId)
        assertEquals(saved.cleanServiceEnabled, found.cleanServiceEnabled)
    }
}
