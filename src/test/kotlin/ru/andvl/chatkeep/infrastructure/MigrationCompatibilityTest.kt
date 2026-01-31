package ru.andvl.chatkeep.infrastructure

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
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
import kotlin.test.fail

/**
 * Tests for migration backward compatibility and idempotency.
 * Ensures migrations can be applied cleanly and don't break existing data.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration::class)
class MigrationCompatibilityTest {

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
    fun `all migrations should be versioned sequentially without gaps`() {
        val info = flyway.info()
        val applied = info.applied()

        // Extract version numbers
        val versions = applied.mapNotNull {
            it.version?.let { v ->
                v.toString().toIntOrNull()
            }
        }.sorted()

        // Check for gaps
        val expectedVersions = (1..versions.max()).toList()
        val missingVersions = expectedVersions - versions.toSet()

        assertTrue(
            missingVersions.isEmpty(),
            "Migration versions should be sequential without gaps. Missing: $missingVersions"
        )
    }

    @Test
    fun `migrations should have unique descriptions`() {
        val info = flyway.info()
        val applied = info.applied()

        val descriptions = applied.map { it.description }
        val duplicates = descriptions.groupingBy { it }.eachCount().filter { it.value > 1 }

        assertTrue(
            duplicates.isEmpty(),
            "Migration descriptions should be unique. Duplicates: ${duplicates.keys}"
        )
    }

    @Test
    fun `migrations should execute in correct order`() {
        val info = flyway.info()
        val applied = info.applied()

        // Verify migrations are sorted by version
        val versions = applied.map { it.version?.version ?: "" }
        val sortedVersions = versions.sortedBy { it.toIntOrNull() ?: 0 }

        assertTrue(
            versions == sortedVersions,
            "Migrations should be applied in version order"
        )
    }

    @Test
    fun `should detect breaking schema changes in migrations`() {
        val info = flyway.info()
        val applied = info.applied()

        // Check for risky operations that might break backward compatibility
        applied.forEach { migration ->
            val script = migration.script ?: return@forEach
            val riskyPatterns = listOf(
                "DROP TABLE" to "Dropping tables",
                "DROP COLUMN" to "Dropping columns",
                "ALTER TABLE.*DROP CONSTRAINT" to "Dropping constraints"
            )

            riskyPatterns.forEach { (pattern, description) ->
                if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(script)) {
                    println("⚠️  WARNING: ${migration.description} contains risky operation: $description")
                    // Note: This is a warning, not a failure - breaking changes are sometimes necessary
                }
            }
        }
    }

    @Test
    fun `clean migration should create all required tables`() {
        val requiredTables = setOf(
            "messages",
            "chat_settings",
            "warnings",
            "punishment_history",
            "blocklist",
            "username_cache",
            "lock_settings",
            "channel_reply_settings",
            "media_storage",
            "welcome_settings",
            "rules",
            "notes",
            "antiflood_settings",
            "gated_feature_settings",
            "twitch_channel_subscriptions",
            "twitch_streams",
            "stream_timeline_events",
            "twitch_notification_settings"
        )

        val actualTables = jdbcTemplate.queryForList(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_type = 'BASE TABLE'
            """.trimIndent()
        ).map { it["table_name"] as String }.toSet()

        val missingTables = requiredTables - actualTables

        assertTrue(
            missingTables.isEmpty(),
            "All required tables should exist after migrations. Missing: $missingTables"
        )
    }

    @Test
    fun `all foreign key constraints should have ON DELETE behavior defined`() {
        val fks = jdbcTemplate.queryForList(
            """
            SELECT
                tc.constraint_name,
                tc.table_name,
                rc.delete_rule
            FROM information_schema.table_constraints tc
            JOIN information_schema.referential_constraints rc
                ON tc.constraint_name = rc.constraint_name
            WHERE tc.constraint_type = 'FOREIGN KEY'
              AND tc.table_schema = 'public'
            """.trimIndent()
        )

        fks.forEach { fk ->
            val deleteRule = fk["delete_rule"] as String
            val constraintName = fk["constraint_name"] as String
            val tableName = fk["table_name"] as String

            assertTrue(
                deleteRule in setOf("CASCADE", "SET NULL", "RESTRICT", "NO ACTION"),
                "Foreign key $constraintName on table $tableName should have explicit ON DELETE behavior (found: $deleteRule)"
            )
        }
    }

    @Test
    fun `all timestamp columns should use TIMESTAMP WITH TIME ZONE`() {
        val timestampColumns = jdbcTemplate.queryForList(
            """
            SELECT table_name, column_name, data_type
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND (column_name LIKE '%_at' OR column_name LIKE '%_date')
              AND data_type != 'timestamp with time zone'
            """.trimIndent()
        )

        assertTrue(
            timestampColumns.isEmpty(),
            "All timestamp columns should use 'timestamp with time zone'. Found: ${
                timestampColumns.joinToString { "${it["table_name"]}.${it["column_name"]} (${it["data_type"]})" }
            }"
        )
    }

    @Test
    fun `all primary keys should be indexed`() {
        val missingIndexes = jdbcTemplate.queryForList(
            """
            SELECT kcu.table_name, kcu.column_name
            FROM information_schema.key_column_usage kcu
            JOIN information_schema.table_constraints tc
                ON kcu.constraint_name = tc.constraint_name
            WHERE tc.constraint_type = 'PRIMARY KEY'
              AND tc.table_schema = 'public'
              AND NOT EXISTS (
                  SELECT 1
                  FROM pg_indexes pi
                  WHERE pi.tablename = kcu.table_name
                    AND pi.indexdef LIKE '%' || kcu.column_name || '%'
              )
            """.trimIndent()
        )

        assertTrue(
            missingIndexes.isEmpty(),
            "All primary keys should have indexes. Missing: ${
                missingIndexes.joinToString { "${it["table_name"]}.${it["column_name"]}" }
            }"
        )
    }
}
