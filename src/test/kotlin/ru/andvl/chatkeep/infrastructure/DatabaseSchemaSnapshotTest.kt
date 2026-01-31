package ru.andvl.chatkeep.infrastructure

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Disabled
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
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Snapshot tests for database schema.
 * Generates a schema snapshot and compares it with the saved snapshot file.
 * If schema changes are detected, the test fails and prompts to regenerate the snapshot.
 *
 * To regenerate snapshot after intentional schema changes:
 * 1. Review the schema diff carefully
 * 2. Set REGENERATE_SNAPSHOT=true environment variable
 * 3. Run the test: ./gradlew test --tests DatabaseSchemaSnapshotTest
 * 4. Commit the updated snapshot file
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration::class)
class DatabaseSchemaSnapshotTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("chatkeep_test")
            .withUsername("test")
            .withPassword("test")

        private val SNAPSHOT_FILE = File("src/test/resources/db-schema-snapshot.txt")
        private val REGENERATE = System.getenv("REGENERATE_SNAPSHOT")?.toBoolean() ?: false
    }

    @Autowired
    private lateinit var flyway: Flyway

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @Disabled("Snapshot not yet generated. Run with REGENERATE_SNAPSHOT=true to create initial snapshot")
    fun `database schema should match snapshot`() {
        val currentSchema = generateSchemaSnapshot()

        if (REGENERATE) {
            SNAPSHOT_FILE.parentFile.mkdirs()
            SNAPSHOT_FILE.writeText(currentSchema)
            println("✓ Schema snapshot regenerated at: ${SNAPSHOT_FILE.path}")
            println("⚠️  Review the changes and commit the updated snapshot file!")
            return
        }

        if (!SNAPSHOT_FILE.exists()) {
            fail(
                """
                Schema snapshot file not found: ${SNAPSHOT_FILE.path}

                To create the initial snapshot, run:
                REGENERATE_SNAPSHOT=true ./gradlew test --tests DatabaseSchemaSnapshotTest

                Then commit the generated snapshot file.
                """.trimIndent()
            )
        }

        val savedSchema = SNAPSHOT_FILE.readText()

        if (currentSchema != savedSchema) {
            val diff = computeDiff(savedSchema, currentSchema)
            fail(
                """
                Database schema has changed!

                ${diff}

                If these changes are intentional, regenerate the snapshot:
                REGENERATE_SNAPSHOT=true ./gradlew test --tests DatabaseSchemaSnapshotTest

                Then review and commit the changes.
                """.trimIndent()
            )
        }
    }

    private fun generateSchemaSnapshot(): String {
        val snapshot = StringBuilder()

        // Add header
        snapshot.appendLine("=".repeat(80))
        snapshot.appendLine("DATABASE SCHEMA SNAPSHOT")
        snapshot.appendLine("Generated from Flyway migrations")
        snapshot.appendLine("=".repeat(80))
        snapshot.appendLine()

        // Add migration versions
        val migrations = flyway.info().applied()
        snapshot.appendLine("APPLIED MIGRATIONS (${migrations.size}):")
        migrations.forEach { migration ->
            snapshot.appendLine("  ${migration.version} - ${migration.description}")
        }
        snapshot.appendLine()

        // Add tables
        val tables = jdbcTemplate.queryForList(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_type = 'BASE TABLE'
            ORDER BY table_name
            """.trimIndent()
        ).map { it["table_name"] as String }

        snapshot.appendLine("TABLES (${tables.size}):")
        tables.forEach { tableName ->
            snapshot.appendLine("-".repeat(80))
            snapshot.appendLine("TABLE: $tableName")
            snapshot.appendLine()

            // Columns
            val columns = jdbcTemplate.queryForList(
                """
                SELECT
                    column_name,
                    data_type,
                    character_maximum_length,
                    is_nullable,
                    column_default
                FROM information_schema.columns
                WHERE table_name = ?
                ORDER BY ordinal_position
                """.trimIndent(),
                tableName
            )

            snapshot.appendLine("  COLUMNS (${columns.size}):")
            columns.forEach { col ->
                val name = col["column_name"] as String
                val type = col["data_type"] as String
                val maxLength = col["character_maximum_length"]?.toString()
                val nullable = col["is_nullable"] as String
                val default = col["column_default"]?.toString()

                val typeStr = if (maxLength != null) "$type($maxLength)" else type
                val nullStr = if (nullable == "YES") "NULL" else "NOT NULL"
                val defaultStr = if (default != null) " DEFAULT $default" else ""

                snapshot.appendLine("    $name: $typeStr $nullStr$defaultStr")
            }
            snapshot.appendLine()

            // Indexes
            val indexes = jdbcTemplate.queryForList(
                """
                SELECT indexname, indexdef
                FROM pg_indexes
                WHERE tablename = ?
                ORDER BY indexname
                """.trimIndent(),
                tableName
            )

            if (indexes.isNotEmpty()) {
                snapshot.appendLine("  INDEXES (${indexes.size}):")
                indexes.forEach { idx ->
                    snapshot.appendLine("    ${idx["indexname"]}")
                    snapshot.appendLine("      ${idx["indexdef"]}")
                }
                snapshot.appendLine()
            }

            // Constraints
            val constraints = jdbcTemplate.queryForList(
                """
                SELECT
                    tc.constraint_name,
                    tc.constraint_type,
                    CASE
                        WHEN tc.constraint_type = 'FOREIGN KEY' THEN
                            (SELECT rc.delete_rule
                             FROM information_schema.referential_constraints rc
                             WHERE rc.constraint_name = tc.constraint_name)
                        ELSE NULL
                    END as delete_rule
                FROM information_schema.table_constraints tc
                WHERE tc.table_name = ?
                  AND tc.table_schema = 'public'
                ORDER BY tc.constraint_type, tc.constraint_name
                """.trimIndent(),
                tableName
            )

            if (constraints.isNotEmpty()) {
                snapshot.appendLine("  CONSTRAINTS (${constraints.size}):")
                constraints.forEach { con ->
                    val name = con["constraint_name"] as String
                    val type = con["constraint_type"] as String
                    val deleteRule = con["delete_rule"]?.toString()

                    val ruleStr = if (deleteRule != null) " ON DELETE $deleteRule" else ""
                    snapshot.appendLine("    $type: $name$ruleStr")
                }
                snapshot.appendLine()
            }
        }

        snapshot.appendLine("=".repeat(80))
        return snapshot.toString()
    }

    private fun computeDiff(old: String, new: String): String {
        val oldLines = old.lines()
        val newLines = new.lines()

        val diff = StringBuilder()
        diff.appendLine("SCHEMA DIFF:")
        diff.appendLine()

        // Simple line-by-line diff
        val maxLines = maxOf(oldLines.size, newLines.size)
        var diffCount = 0

        for (i in 0 until maxLines) {
            val oldLine = oldLines.getOrNull(i)
            val newLine = newLines.getOrNull(i)

            when {
                oldLine != null && newLine == null -> {
                    diff.appendLine("- $oldLine")
                    diffCount++
                }
                oldLine == null && newLine != null -> {
                    diff.appendLine("+ $newLine")
                    diffCount++
                }
                oldLine != newLine -> {
                    diff.appendLine("- $oldLine")
                    diff.appendLine("+ $newLine")
                    diffCount++
                }
            }

            // Limit diff output
            if (diffCount > 50) {
                diff.appendLine("... (diff truncated, ${maxLines - i} more lines)")
                break
            }
        }

        return diff.toString()
    }

    @Test
    fun `snapshot file should exist or be regeneratable`() {
        if (!SNAPSHOT_FILE.exists() && !REGENERATE) {
            fail(
                """
                Schema snapshot not found and REGENERATE_SNAPSHOT is not set.

                Run with REGENERATE_SNAPSHOT=true to create initial snapshot:
                REGENERATE_SNAPSHOT=true ./gradlew test --tests DatabaseSchemaSnapshotTest
                """.trimIndent()
            )
        }
    }
}
