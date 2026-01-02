package ru.andvl.chatkeep.bot.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.databind.ObjectMapper
import ru.andvl.chatkeep.domain.model.moderation.MatchType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for RoseImportParser.
 *
 * Tests the Rose Bot export file parsing functionality:
 * - JSON structure parsing with nested objects
 * - Action parsing from {action} and {action duration} formats
 * - Duration parsing for tmute actions
 * - Severity calculation based on punishment type
 * - Match type detection (exact vs wildcard)
 * - Edge cases: empty files, invalid JSON, missing fields, oversized patterns
 */
class RoseImportParserTest {

    private val objectMapper = ObjectMapper()

    // HAPPY PATH TESTS - BASIC PARSING

    @Test
    fun `parse should handle valid Rose export with single filter`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {
                                "name": "spam",
                                "reason": "{ban}"
                            }
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(1, result.items.size, "Should parse 1 filter")
        assertEquals(0, result.skippedCount, "Should skip 0 filters")

        val item = result.items.first()
        assertEquals("spam", item.pattern)
        assertEquals(PunishmentType.BAN, item.action)
        assertEquals(null, item.durationHours)
        assertEquals(5, item.severity) // BAN = severity 5
        assertEquals(MatchType.EXACT, item.matchType)
    }

    @Test
    fun `parse should handle multiple filters`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "spam", "reason": "{ban}"},
                            {"name": "bad*word", "reason": "{kick}"},
                            {"name": "mild", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(3, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    // ACTION PARSING TESTS

    @ParameterizedTest
    @CsvSource(
        "'{ban}',       BAN",
        "'{sban}',      BAN",     // Silent ban
        "'{kick}',      KICK",
        "'{warn}',      WARN",
        "'{mute}',      MUTE",
        "'{nothing}',   NOTHING",
        "'{BAN}',       BAN",     // Uppercase
        "'{Ban}',       BAN",     // Mixed case
        delimiter = ','
    )
    fun `parse should correctly map Rose actions to PunishmentType`(
        roseAction: String,
        expectedAction: String
    ) {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": "$roseAction"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(1, result.items.size)
        assertEquals(PunishmentType.valueOf(expectedAction), result.items.first().action)
    }

    @Test
    fun `parse should default to WARN for empty reason`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": ""}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(PunishmentType.WARN, result.items.first().action)
        assertEquals(null, result.items.first().durationHours)
    }

    @Test
    fun `parse should default to WARN for missing reason field`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(PunishmentType.WARN, result.items.first().action)
    }

    @Test
    fun `parse should default to WARN for invalid action`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": "{unknown_action}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(PunishmentType.WARN, result.items.first().action)
    }

    @Test
    fun `parse should extract action from reason with extra text`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": "This is spam {ban} do not use"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(PunishmentType.BAN, result.items.first().action)
    }

    // DURATION PARSING TESTS

    @ParameterizedTest
    @CsvSource(
        "'{tmute 1h}',   1",
        "'{tmute 2h}',   2",
        "'{tmute 24h}',  24",
        "'{tmute 1d}',   24",
        "'{tmute 7d}',   168",
        "'{tmute 30m}',  0",     // 30 minutes = 0 hours (truncated)
        "'{tmute 90m}',  1",     // 90 minutes = 1 hour
        delimiter = ','
    )
    fun `parse should extract duration from tmute action`(
        roseAction: String,
        expectedHours: Int
    ) {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": "$roseAction"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(PunishmentType.MUTE, result.items.first().action)
        assertEquals(expectedHours, result.items.first().durationHours)
    }

    @Test
    fun `parse should set null duration for tmute without duration`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": "{tmute}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(PunishmentType.MUTE, result.items.first().action)
        assertEquals(null, result.items.first().durationHours)
    }

    @Test
    fun `parse should set null duration for tmute with invalid duration`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": "{tmute invalid}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(PunishmentType.MUTE, result.items.first().action)
        assertEquals(null, result.items.first().durationHours)
    }

    @Test
    fun `parse should not set duration for non-tmute actions`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test1", "reason": "{ban}"},
                            {"name": "test2", "reason": "{kick}"},
                            {"name": "test3", "reason": "{warn}"},
                            {"name": "test4", "reason": "{mute}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(4, result.items.size)
        result.items.forEach { item ->
            assertEquals(null, item.durationHours, "Non-tmute actions should have null duration")
        }
    }

    // SEVERITY CALCULATION TESTS

    @ParameterizedTest
    @CsvSource(
        "BAN,     5",
        "KICK,    4",
        "MUTE,    3",
        "WARN,    2",
        "NOTHING, 1",
        delimiter = ','
    )
    fun `parse should calculate correct severity for each action type`(
        actionType: String,
        expectedSeverity: Int
    ) {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": "{${actionType.lowercase()}}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(expectedSeverity, result.items.first().severity)
    }

    // MATCH TYPE DETECTION TESTS

    @Test
    fun `parse should detect EXACT match type for patterns without wildcards`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "spam", "reason": "{warn}"},
                            {"name": "bad word", "reason": "{warn}"},
                            {"name": "123", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(3, result.items.size)
        result.items.forEach { item ->
            assertEquals(MatchType.EXACT, item.matchType)
        }
    }

    @Test
    fun `parse should detect WILDCARD match type for patterns with asterisk`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "spam*", "reason": "{warn}"},
                            {"name": "*bad*", "reason": "{warn}"},
                            {"name": "test*word", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(3, result.items.size)
        result.items.forEach { item ->
            assertEquals(MatchType.WILDCARD, item.matchType)
        }
    }

    @Test
    fun `parse should detect WILDCARD match type for patterns with question mark`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "sp?m", "reason": "{warn}"},
                            {"name": "???", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(2, result.items.size)
        result.items.forEach { item ->
            assertEquals(MatchType.WILDCARD, item.matchType)
        }
    }

    @Test
    fun `parse should detect WILDCARD for patterns with both wildcards`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "s*a?", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(MatchType.WILDCARD, result.items.first().matchType)
    }

    // PATTERN TRIMMING AND VALIDATION TESTS

    @Test
    fun `parse should trim whitespace from patterns`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "  spam  ", "reason": "{warn}"},
                            {"name": "\ttest\t", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(2, result.items.size)
        assertEquals("spam", result.items[0].pattern)
        assertEquals("test", result.items[1].pattern)
    }

    @Test
    fun `parse should skip empty patterns`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "", "reason": "{warn}"},
                            {"name": "   ", "reason": "{warn}"},
                            {"name": "valid", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(1, result.items.size, "Should only parse non-empty pattern")
        assertEquals(2, result.skippedCount, "Should skip 2 empty patterns")
        assertEquals("valid", result.items.first().pattern)
    }

    @Test
    fun `parse should skip null patterns`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"reason": "{warn}"},
                            {"name": "valid", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(1, result.items.size)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun `parse should skip patterns exceeding MAX_PATTERN_LENGTH`() {
        // Given - Pattern with 501 characters (exceeds 500 limit)
        val longPattern = "a".repeat(501)
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "$longPattern", "reason": "{warn}"},
                            {"name": "valid", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(1, result.items.size, "Should skip oversized pattern")
        assertEquals(1, result.skippedCount)
        assertEquals("valid", result.items.first().pattern)
    }

    @Test
    fun `parse should accept pattern at MAX_PATTERN_LENGTH boundary`() {
        // Given - Pattern with exactly 500 characters
        val maxPattern = "a".repeat(500)
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "$maxPattern", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(1, result.items.size, "Should accept pattern at max length")
        assertEquals(0, result.skippedCount)
        assertEquals(500, result.items.first().pattern.length)
    }

    // EDGE CASES - MALFORMED JSON

    @Test
    fun `parse should handle empty JSON object`() {
        // Given
        val json = "{}"

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle JSON with null data field`() {
        // Given
        val json = """{"data": null}"""

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle JSON with missing data field`() {
        // Given
        val json = """{"other_field": "value"}"""

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle JSON with null blocklists field`() {
        // Given
        val json = """{"data": {"blocklists": null}}"""

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle JSON with missing blocklists field`() {
        // Given
        val json = """{"data": {"other": "value"}}"""

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle JSON with null filters array`() {
        // Given
        val json = """{"data": {"blocklists": {"filters": null}}}"""

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle JSON with empty filters array`() {
        // Given
        val json = """{"data": {"blocklists": {"filters": []}}}"""

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle invalid JSON syntax`() {
        // Given - Malformed JSON
        val json = """{"data": {"blocklists": {filters: ["""

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then - Should return empty result instead of throwing
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle completely invalid input`() {
        // Given
        val json = "this is not json at all"

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle empty string`() {
        // Given
        val json = ""

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle whitespace-only string`() {
        // Given
        val json = "   \n\t  "

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(0, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    // EDGE CASES - UNUSUAL BUT VALID DATA

    @Test
    fun `parse should handle filters with extra unknown fields`() {
        // Given - Rose might add new fields in future exports
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {
                                "name": "spam",
                                "reason": "{ban}",
                                "created_at": "2024-01-01",
                                "creator_id": 123,
                                "unknown_field": "value"
                            }
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then - Should parse successfully, ignoring unknown fields
        assertEquals(1, result.items.size)
        assertEquals("spam", result.items.first().pattern)
        assertEquals(PunishmentType.BAN, result.items.first().action)
    }

    @Test
    fun `parse should handle special characters in patterns`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "@#$%^&*()", "reason": "{warn}"},
                            {"name": "test\nline", "reason": "{warn}"},
                            {"name": "émojis😀", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(3, result.items.size)
        assertEquals("@#$%^&*()", result.items[0].pattern)
    }

    @Test
    fun `parse should handle Unicode patterns`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "Привет", "reason": "{warn}"},
                            {"name": "你好", "reason": "{warn}"},
                            {"name": "مرحبا", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(3, result.items.size)
        assertEquals("Привет", result.items[0].pattern)
        assertEquals("你好", result.items[1].pattern)
        assertEquals("مرحبا", result.items[2].pattern)
    }

    // COMPREHENSIVE SCENARIO TESTS

    @Test
    fun `parse complete realistic Rose export`() {
        // Given - Realistic export with various action types and patterns
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "spam", "reason": "Spam content {ban}"},
                            {"name": "bad*", "reason": "{kick}"},
                            {"name": "mild", "reason": "{warn}"},
                            {"name": "flood", "reason": "{tmute 1h}"},
                            {"name": "caps*lock", "reason": "{tmute 30m}"},
                            {"name": "", "reason": "{warn}"},
                            {"name": "ignored", "reason": "{nothing}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(6, result.items.size, "Should parse 6 valid filters")
        assertEquals(1, result.skippedCount, "Should skip 1 empty pattern")

        // Verify each filter
        val spam = result.items.find { it.pattern == "spam" }
        assertNotNull(spam)
        assertEquals(PunishmentType.BAN, spam.action)
        assertEquals(5, spam.severity)
        assertEquals(MatchType.EXACT, spam.matchType)

        val bad = result.items.find { it.pattern == "bad*" }
        assertNotNull(bad)
        assertEquals(PunishmentType.KICK, bad.action)
        assertEquals(4, bad.severity)
        assertEquals(MatchType.WILDCARD, bad.matchType)

        val flood = result.items.find { it.pattern == "flood" }
        assertNotNull(flood)
        assertEquals(PunishmentType.MUTE, flood.action)
        assertEquals(1, flood.durationHours)

        val caps = result.items.find { it.pattern == "caps*lock" }
        assertNotNull(caps)
        assertEquals(PunishmentType.MUTE, caps.action)
        assertEquals(0, caps.durationHours) // 30m truncates to 0 hours

        val ignored = result.items.find { it.pattern == "ignored" }
        assertNotNull(ignored)
        assertEquals(PunishmentType.NOTHING, ignored.action)
        assertEquals(1, ignored.severity)
    }

    @Test
    fun `parse should handle mixed valid and invalid filters`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "valid1", "reason": "{warn}"},
                            {"name": "", "reason": "{warn}"},
                            {"name": "valid2", "reason": "{ban}"},
                            {"name": "   ", "reason": "{kick}"},
                            {"name": "${"x".repeat(501)}", "reason": "{warn}"},
                            {"name": "valid3", "reason": "{mute}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(3, result.items.size, "Should parse 3 valid filters")
        assertEquals(3, result.skippedCount, "Should skip 3 invalid filters")

        val patterns = result.items.map { it.pattern }
        assertTrue(patterns.contains("valid1"))
        assertTrue(patterns.contains("valid2"))
        assertTrue(patterns.contains("valid3"))
    }

    @Test
    fun `parse should preserve pattern order`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "first", "reason": "{warn}"},
                            {"name": "second", "reason": "{warn}"},
                            {"name": "third", "reason": "{warn}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(3, result.items.size)
        assertEquals("first", result.items[0].pattern)
        assertEquals("second", result.items[1].pattern)
        assertEquals("third", result.items[2].pattern)
    }

    // BOUNDARY TESTS

    @Test
    fun `parse should handle very long valid JSON`() {
        // Given - Generate 100 filters
        val filters = (1..100).joinToString(",") { i ->
            """{"name": "pattern$i", "reason": "{warn}"}"""
        }
        val json = """{"data": {"blocklists": {"filters": [$filters]}}}"""

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then
        assertEquals(100, result.items.size)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `parse should handle reason with multiple braces`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": "Text {ban} more {text}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then - Should extract first action
        assertEquals(PunishmentType.BAN, result.items.first().action)
    }

    @Test
    fun `parse should handle nested braces in reason`() {
        // Given
        val json = """
            {
                "data": {
                    "blocklists": {
                        "filters": [
                            {"name": "test", "reason": "{{ban}}"}
                        ]
                    }
                }
            }
        """.trimIndent()

        // When
        val result = RoseImportParser.parse(json, objectMapper)

        // Then - Regex should still extract {ban}
        assertEquals(1, result.items.size)
    }
}
