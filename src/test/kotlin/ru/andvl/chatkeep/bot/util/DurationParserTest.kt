package ru.andvl.chatkeep.bot.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Unit tests for DurationParser.
 * Validates duration parsing for moderation commands.
 */
class DurationParserTest {

    @ParameterizedTest
    @CsvSource(
        "1m, 60",          // 1 minute = 60 seconds
        "5m, 300",         // 5 minutes = 300 seconds
        "1h, 3600",        // 1 hour = 3600 seconds
        "2h, 7200",        // 2 hours
        "12h, 43200",      // 12 hours
        "1d, 86400",       // 1 day = 86400 seconds
        "7d, 604800",      // 7 days
        "30d, 2592000"     // 30 days
    )
    fun `parse should correctly parse valid durations`(input: String, expectedSeconds: Long) {
        // When
        val result = DurationParser.parse(input)

        // Then
        assertEquals(expectedSeconds, result?.inWholeSeconds)
    }

    @Test
    fun `parse should handle uppercase input`() {
        // When
        val result1 = DurationParser.parse("5M")
        val result2 = DurationParser.parse("2H")
        val result3 = DurationParser.parse("3D")

        // Then
        assertEquals(5.minutes.inWholeSeconds, result1?.inWholeSeconds)
        assertEquals(2.hours.inWholeSeconds, result2?.inWholeSeconds)
        assertEquals(3.days.inWholeSeconds, result3?.inWholeSeconds)
    }

    @Test
    fun `parse should handle mixed case input`() {
        // When
        val result = DurationParser.parse("10H")

        // Then
        assertEquals(10.hours.inWholeSeconds, result?.inWholeSeconds)
    }

    @ParameterizedTest
    @CsvSource(
        "0m",       // Zero duration (valid but edge case)
        "1000h",    // Large hour value
        "999d"      // Large day value
    )
    fun `parse should handle edge case valid inputs`(input: String) {
        // When
        val result = DurationParser.parse(input)

        // Then
        assertNotNull(result, "Should parse valid format even with unusual values: $input")
    }

    @Test
    fun `parse should return null for invalid inputs`() {
        val invalidInputs = listOf(
            "5",            // No unit
            "m",            // No number
            "5s",           // Unsupported unit (seconds)
            "5w",           // Unsupported unit (weeks)
            "5y",           // Unsupported unit (years)
            "5 m",          // Space between number and unit
            "5min",         // Invalid unit format
            "5hours",       // Invalid unit format
            "1h30m",        // Multiple units not supported
            "abc",          // Invalid input
            "",             // Empty string
            "  ",           // Whitespace only
            "-5h",          // Negative duration
            "1.5h"          // Decimal not supported
        )

        invalidInputs.forEach { input ->
            val result = DurationParser.parse(input)
            assertNull(result, "Should return null for invalid input: $input")
        }
    }

    @Test
    fun `toSeconds should convert duration to seconds`() {
        // Given
        val duration = 2.hours + 30.minutes

        // When
        val seconds = DurationParser.toSeconds(duration)

        // Then
        assertEquals(9000L, seconds) // 2*3600 + 30*60 = 9000
    }

    @Test
    fun `toHours should convert duration to hours`() {
        // Given
        val duration1 = 3.hours
        val duration2 = 90.minutes
        val duration3 = 2.days

        // When
        val hours1 = DurationParser.toHours(duration1)
        val hours2 = DurationParser.toHours(duration2)
        val hours3 = DurationParser.toHours(duration3)

        // Then
        assertEquals(3, hours1)
        assertEquals(1, hours2) // Truncates to 1 hour
        assertEquals(48, hours3)
    }

    @Test
    fun `toHours should truncate partial hours`() {
        // Given
        val duration = 1.hours + 59.minutes

        // When
        val hours = DurationParser.toHours(duration)

        // Then
        assertEquals(1, hours, "Should truncate 1h59m to 1 hour")
    }

    @Test
    fun `parse should handle zero duration`() {
        // When
        val result = DurationParser.parse("0m")

        // Then
        assertEquals(0L, result?.inWholeSeconds)
    }

    @Test
    fun `parse should handle large numbers`() {
        // When
        val result1 = DurationParser.parse("9999h")
        val result2 = DurationParser.parse("365d")

        // Then
        kotlin.test.assertNotNull(result1)
        assertEquals(9999 * 3600L, result1.inWholeSeconds)
        kotlin.test.assertNotNull(result2)
        assertEquals(365 * 86400L, result2.inWholeSeconds)
    }

    private fun assertNotNull(value: Any?, message: String = "Value should not be null") {
        kotlin.test.assertNotNull(value, message)
    }
}
