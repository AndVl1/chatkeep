package ru.andvl.chatkeep.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.andvl.chatkeep.config.TestConfiguration
import kotlin.test.assertTrue

/**
 * Tests for Flyway database migrations.
 * Verifies schema is created correctly with all tables, indexes, and constraints.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration::class)
class FlywayMigrationTest {

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
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `all migrations should apply successfully`() {
        val info = flyway.info()
        val applied = info.applied()

        assertTrue(applied.isNotEmpty(), "Should have applied migrations")
        assertTrue(applied.all { it.state.isApplied }, "All migrations should be applied")
    }

    @Test
    fun `messages table should exist with correct columns`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'messages'
            ORDER BY ordinal_position
            """.trimIndent()
        )

        assertTrue(columns.isNotEmpty(), "messages table should exist")

        val columnNames = columns.map { it["column_name"] as String }
        assertTrue("id" in columnNames, "Should have id column")
        assertTrue("telegram_message_id" in columnNames, "Should have telegram_message_id column")
        assertTrue("chat_id" in columnNames, "Should have chat_id column")
        assertTrue("user_id" in columnNames, "Should have user_id column")
        assertTrue("username" in columnNames, "Should have username column")
        assertTrue("first_name" in columnNames, "Should have first_name column")
        assertTrue("last_name" in columnNames, "Should have last_name column")
        assertTrue("text" in columnNames, "Should have text column")
        assertTrue("message_date" in columnNames, "Should have message_date column")
        assertTrue("created_at" in columnNames, "Should have created_at column")
    }

    @Test
    fun `chat_settings table should exist with correct columns`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'chat_settings'
            ORDER BY ordinal_position
            """.trimIndent()
        )

        assertTrue(columns.isNotEmpty(), "chat_settings table should exist")

        val columnNames = columns.map { it["column_name"] as String }
        assertTrue("id" in columnNames, "Should have id column")
        assertTrue("chat_id" in columnNames, "Should have chat_id column")
        assertTrue("chat_title" in columnNames, "Should have chat_title column")
        assertTrue("collection_enabled" in columnNames, "Should have collection_enabled column")
        assertTrue("created_at" in columnNames, "Should have created_at column")
        assertTrue("updated_at" in columnNames, "Should have updated_at column")
    }

    @Test
    fun `messages table should have required indexes`() {
        val indexes = jdbcTemplate.queryForList(
            """
            SELECT indexname
            FROM pg_indexes
            WHERE tablename = 'messages'
            """.trimIndent()
        ).map { it["indexname"] as String }

        assertTrue(indexes.any { it.contains("chat_id") }, "Should have index on chat_id")
        assertTrue(indexes.any { it.contains("user_id") }, "Should have index on user_id")
        assertTrue(indexes.any { it.contains("date") }, "Should have index on message_date")
    }

    @Test
    fun `chat_settings table should have index on chat_id`() {
        val indexes = jdbcTemplate.queryForList(
            """
            SELECT indexname
            FROM pg_indexes
            WHERE tablename = 'chat_settings'
            """.trimIndent()
        ).map { it["indexname"] as String }

        assertTrue(indexes.any { it.contains("chat_id") }, "Should have index on chat_id")
    }

    @Test
    fun `messages table should have unique constraint on chat_id and telegram_message_id`() {
        val constraints = jdbcTemplate.queryForList(
            """
            SELECT constraint_name, constraint_type
            FROM information_schema.table_constraints
            WHERE table_name = 'messages' AND constraint_type = 'UNIQUE'
            """.trimIndent()
        )

        assertTrue(constraints.isNotEmpty(), "Should have unique constraint")
        assertTrue(
            constraints.any { (it["constraint_name"] as String).contains("chat_message") },
            "Should have unique constraint on chat_id and telegram_message_id"
        )
    }

    @Test
    fun `chat_settings should have unique constraint on chat_id`() {
        val constraints = jdbcTemplate.queryForList(
            """
            SELECT constraint_name, constraint_type
            FROM information_schema.table_constraints
            WHERE table_name = 'chat_settings' AND constraint_type = 'UNIQUE'
            """.trimIndent()
        )

        assertTrue(constraints.isNotEmpty(), "Should have unique constraint on chat_id")
    }

    @Test
    fun `flyway schema history table should exist`() {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history",
            Long::class.java
        )

        assertTrue(count!! >= 1, "Should have at least one migration record")
    }

    @Test
    fun `twitch_streams table should exist with correct columns`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'twitch_streams'
            ORDER BY ordinal_position
            """.trimIndent()
        )

        assertTrue(columns.isNotEmpty(), "twitch_streams table should exist")

        val columnNames = columns.map { it["column_name"] as String }
        assertTrue("id" in columnNames, "Should have id column")
        assertTrue("subscription_id" in columnNames, "Should have subscription_id column")
        assertTrue("twitch_stream_id" in columnNames, "Should have twitch_stream_id column")
        assertTrue("started_at" in columnNames, "Should have started_at column")
        assertTrue("ended_at" in columnNames, "Should have ended_at column")
        assertTrue("status" in columnNames, "Should have status column")
        assertTrue("telegram_message_id" in columnNames, "Should have telegram_message_id column")
        assertTrue("telegram_chat_id" in columnNames, "Should have telegram_chat_id column")
        assertTrue("telegraph_url" in columnNames, "Should have telegraph_url column")
        assertTrue("has_photo" in columnNames, "Should have has_photo column")
    }

    @Test
    fun `stream_timeline_events table should exist with correct columns`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'stream_timeline_events'
            ORDER BY ordinal_position
            """.trimIndent()
        )

        assertTrue(columns.isNotEmpty(), "stream_timeline_events table should exist")

        val columnNames = columns.map { it["column_name"] as String }
        assertTrue("id" in columnNames, "Should have id column")
        assertTrue("stream_id" in columnNames, "Should have stream_id column")
        assertTrue("event_time" in columnNames, "Should have event_time column")
        assertTrue("stream_offset_seconds" in columnNames, "Should have stream_offset_seconds column")
        assertTrue("game_name" in columnNames, "Should have game_name column")
        assertTrue("stream_title" in columnNames, "Should have stream_title column")
    }

    @Test
    fun `twitch_channel_subscriptions table should have pin columns`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'twitch_channel_subscriptions'
              AND column_name IN ('is_pinned', 'pin_silently')
            ORDER BY ordinal_position
            """.trimIndent()
        )

        assertTrue(columns.isNotEmpty(), "Should have pin columns")

        val columnNames = columns.map { it["column_name"] as String }
        assertTrue("is_pinned" in columnNames, "Should have is_pinned column")
        assertTrue("pin_silently" in columnNames, "Should have pin_silently column")
    }

    @Test
    fun `media_storage table should exist with BYTEA column`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'media_storage'
            ORDER BY ordinal_position
            """.trimIndent()
        )

        assertTrue(columns.isNotEmpty(), "media_storage table should exist")

        val columnNames = columns.map { it["column_name"] as String }
        assertTrue("id" in columnNames, "Should have id column")
        assertTrue("hash" in columnNames, "Should have hash column")
        assertTrue("content" in columnNames, "Should have content column")
        assertTrue("mime_type" in columnNames, "Should have mime_type column")
        assertTrue("file_size" in columnNames, "Should have file_size column")
        assertTrue("telegram_file_id" in columnNames, "Should have telegram_file_id column")

        // Verify content column is BYTEA
        val contentColumn = columns.find { it["column_name"] == "content" }
        assertTrue(
            contentColumn != null && contentColumn["data_type"] == "bytea",
            "content column should be BYTEA type"
        )
    }
}
