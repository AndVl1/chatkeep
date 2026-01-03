package ru.andvl.chatkeep.bot.util

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Comprehensive tests for AddBlockParser.
 *
 * Syntax: /addblock <pattern> {action [duration]}
 *
 * Test categories:
 * 1. Pattern-only parsing (no braces)
 * 2. Action in braces (no duration)
 * 3. Action with duration in braces
 * 4. Error cases
 * 5. Edge cases and special characters
 */
class AddBlockParserTest {

    @Nested
    inner class PatternOnlyTests {

        @Test
        fun `parse simple pattern without braces returns pattern only`() {
            // Given
            val input = "spam"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam", result.parsed.pattern)
            assertNull(result.parsed.action)
            assertNull(result.parsed.durationMinutes)
        }

        @Test
        fun `parse wildcard pattern without braces`() {
            // Given
            val input = "*spam*"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("*spam*", result.parsed.pattern)
            assertNull(result.parsed.action)
        }

        @Test
        fun `parse pattern with spaces is trimmed`() {
            // Given
            val input = "  spam  "

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam", result.parsed.pattern)
        }

        @Test
        fun `parse multi-word pattern without braces`() {
            // Given
            val input = "bad word here"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("bad word here", result.parsed.pattern)
            assertNull(result.parsed.action)
        }

        @ParameterizedTest
        @ValueSource(strings = ["test*", "*test", "te?st", "t*e?s*t"])
        fun `parse patterns with wildcards`(input: String) {
            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals(input, result.parsed.pattern)
        }
    }

    @Nested
    inner class ActionInBracesTests {

        @ParameterizedTest
        @CsvSource(
            "spam {warn}, spam, WARN",
            "badword {mute}, badword, MUTE",
            "scam {ban}, scam, BAN",
            "test {kick}, test, KICK",
            "ad {nothing}, ad, NOTHING"
        )
        fun `parse pattern with action in braces`(input: String, expectedPattern: String, expectedAction: String) {
            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals(expectedPattern, result.parsed.pattern)
            assertEquals(PunishmentType.valueOf(expectedAction), result.parsed.action)
            assertNull(result.parsed.durationMinutes)
        }

        @Test
        fun `parse action is case insensitive`() {
            // Given - lowercase
            val input1 = "spam {warn}"
            // Given - uppercase
            val input2 = "spam {WARN}"
            // Given - mixed case
            val input3 = "spam {Warn}"

            // When
            val result1 = AddBlockParser.parse(input1)
            val result2 = AddBlockParser.parse(input2)
            val result3 = AddBlockParser.parse(input3)

            // Then - all should parse to WARN
            assertIs<AddBlockParser.Result.Success>(result1)
            assertIs<AddBlockParser.Result.Success>(result2)
            assertIs<AddBlockParser.Result.Success>(result3)
            assertEquals(PunishmentType.WARN, result1.parsed.action)
            assertEquals(PunishmentType.WARN, result2.parsed.action)
            assertEquals(PunishmentType.WARN, result3.parsed.action)
        }

        @Test
        fun `parse wildcard pattern with action`() {
            // Given
            val input = "*spam* {ban}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("*spam*", result.parsed.pattern)
            assertEquals(PunishmentType.BAN, result.parsed.action)
        }

        @Test
        fun `parse action with extra spaces in braces`() {
            // Given
            val input = "spam {  warn  }"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam", result.parsed.pattern)
            assertEquals(PunishmentType.WARN, result.parsed.action)
        }
    }

    @Nested
    inner class ActionWithDurationTests {

        @ParameterizedTest
        @CsvSource(
            "spam {mute 1h}, spam, MUTE, 60",
            "spam {mute 2h}, spam, MUTE, 120",
            "spam {mute 24h}, spam, MUTE, 1440",
            "spam {ban 7d}, spam, BAN, 10080",   // 7 * 24 * 60 = 10080 minutes
            "spam {mute 1d}, spam, MUTE, 1440",  // 1 * 24 * 60 = 1440 minutes
            "spam {mute 30m}, spam, MUTE, 30",   // 30 minutes
            "spam {mute 5m}, spam, MUTE, 5"      // 5 minutes - the bug fix!
        )
        fun `parse pattern with action and duration`(
            input: String,
            expectedPattern: String,
            expectedAction: String,
            expectedMinutes: Int
        ) {
            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals(expectedPattern, result.parsed.pattern)
            assertEquals(PunishmentType.valueOf(expectedAction), result.parsed.action)
            assertEquals(expectedMinutes, result.parsed.durationMinutes)
        }

        @Test
        fun `parse duration with uppercase unit`() {
            // Given
            val input = "spam {mute 1H}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals(60, result.parsed.durationMinutes)
        }

        @Test
        fun `parse invalid duration is ignored`() {
            // Given - invalid duration format
            val input = "spam {mute invalid}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam", result.parsed.pattern)
            assertEquals(PunishmentType.MUTE, result.parsed.action)
            assertNull(result.parsed.durationMinutes) // Invalid duration -> null
        }

        @Test
        fun `parse action with duration and extra spaces`() {
            // Given
            val input = "spam {  mute   1h  }"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam", result.parsed.pattern)
            assertEquals(PunishmentType.MUTE, result.parsed.action)
            assertEquals(60, result.parsed.durationMinutes)
        }

        @Test
        fun `parse wildcard pattern with action and duration`() {
            // Given
            val input = "*bad*word* {ban 7d}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("*bad*word*", result.parsed.pattern)
            assertEquals(PunishmentType.BAN, result.parsed.action)
            assertEquals(10080, result.parsed.durationMinutes) // 7 * 24 * 60
        }
    }

    @Nested
    inner class ErrorCaseTests {

        @Test
        fun `parse empty input returns EmptyInput error`() {
            // Given
            val input = ""

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Failure>(result)
            assertIs<AddBlockParser.ParseError.EmptyInput>(result.error)
        }

        @Test
        fun `parse whitespace-only input returns EmptyInput error`() {
            // Given
            val input = "   "

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Failure>(result)
            assertIs<AddBlockParser.ParseError.EmptyInput>(result.error)
        }

        @Test
        fun `parse braces-only input returns EmptyPattern error`() {
            // Given
            val input = "{warn}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Failure>(result)
            assertIs<AddBlockParser.ParseError.EmptyPattern>(result.error)
        }

        @Test
        fun `parse unknown action returns UnknownAction error`() {
            // Given
            val input = "spam {unknown}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Failure>(result)
            val error = result.error
            assertIs<AddBlockParser.ParseError.UnknownAction>(error)
            assertEquals("UNKNOWN", error.actionStr)
        }

        @Test
        fun `parse invalid action returns UnknownAction error with original value`() {
            // Given
            val input = "spam {delete}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Failure>(result)
            val error = result.error
            assertIs<AddBlockParser.ParseError.UnknownAction>(error)
            assertEquals("DELETE", error.actionStr)
        }

        @Test
        fun `parse pattern too long returns PatternTooLong error`() {
            // Given - pattern > 100 characters
            val longPattern = "a".repeat(101)
            val input = longPattern

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Failure>(result)
            val error = result.error
            assertIs<AddBlockParser.ParseError.PatternTooLong>(error)
            assertEquals(101, error.length)
            assertEquals(100, error.maxLength)
        }

        @Test
        fun `parse long pattern with braces still checks length`() {
            // Given - pattern > 100 characters before braces
            val longPattern = "a".repeat(101)
            val input = "$longPattern {warn}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Failure>(result)
            assertIs<AddBlockParser.ParseError.PatternTooLong>(result.error)
        }
    }

    @Nested
    inner class EdgeCaseTests {

        @Test
        fun `parse pattern exactly 100 characters is valid`() {
            // Given
            val pattern = "a".repeat(100)
            val input = pattern

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals(100, result.parsed.pattern.length)
        }

        @Test
        fun `parse pattern with curly braces in pattern - regex takes first match`() {
            // Given - braces in the middle, action braces at end
            val input = "test{text}more {warn}"

            // When
            val result = AddBlockParser.parse(input)

            // Then - First brace match is {text}, pattern is "test", but "text" is invalid action
            assertIs<AddBlockParser.Result.Failure>(result)
            val error = result.error
            assertIs<AddBlockParser.ParseError.UnknownAction>(error)
            assertEquals("TEXT", error.actionStr)
        }

        @Test
        fun `parse unclosed brace is treated as pattern`() {
            // Given
            val input = "spam {warn"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam {warn", result.parsed.pattern)
            assertNull(result.parsed.action)
        }

        @Test
        fun `parse extra closing brace in pattern`() {
            // Given
            val input = "spam} {warn}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam}", result.parsed.pattern)
            assertEquals(PunishmentType.WARN, result.parsed.action)
        }

        @Test
        fun `parse empty braces is treated as pattern (regex requires content)`() {
            // Given - empty braces don't match regex \{([^}]+)\}
            val input = "spam {}"

            // When
            val result = AddBlockParser.parse(input)

            // Then - treated as pattern "spam {}" with no action
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam {}", result.parsed.pattern)
            assertNull(result.parsed.action)
        }

        @Test
        fun `parse braces with only whitespace returns UnknownAction error with empty string`() {
            // Given - spaces inside braces match regex, but trim to empty
            val input = "spam {   }"

            // When
            val result = AddBlockParser.parse(input)

            // Then - empty action string after trim
            assertIs<AddBlockParser.Result.Failure>(result)
            val error = result.error
            assertIs<AddBlockParser.ParseError.UnknownAction>(error)
            assertEquals("", error.actionStr)
        }

        @Test
        fun `parse pattern with special regex characters`() {
            // Given - pattern with regex special chars that should be treated literally
            val input = "test.pattern+here"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("test.pattern+here", result.parsed.pattern)
        }

        @Test
        fun `parse pattern with newlines in input`() {
            // Given
            val input = "spam\nmore"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam\nmore", result.parsed.pattern)
        }

        @Test
        fun `parse pattern with unicode characters`() {
            // Given
            val input = "спам"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("спам", result.parsed.pattern)
        }

        @Test
        fun `parse pattern with unicode and action`() {
            // Given
            val input = "спам {ban}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("спам", result.parsed.pattern)
            assertEquals(PunishmentType.BAN, result.parsed.action)
        }

        @Test
        fun `parse multiple braces takes first match`() {
            // Given
            val input = "spam {warn} {ban}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam", result.parsed.pattern)
            assertEquals(PunishmentType.WARN, result.parsed.action)
        }

        @Test
        fun `parse large duration value`() {
            // Given
            val input = "spam {mute 999d}"

            // When
            val result = AddBlockParser.parse(input)

            // Then
            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam", result.parsed.pattern)
            assertEquals(PunishmentType.MUTE, result.parsed.action)
            assertEquals(999 * 24 * 60, result.parsed.durationMinutes)  // 1438560 minutes
        }
    }

    @Nested
    inner class RealWorldExamplesTests {

        @Test
        fun `parse Rose bot style - addblock spam`() {
            val result = AddBlockParser.parse("spam")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam", result.parsed.pattern)
            assertNull(result.parsed.action)
        }

        @Test
        fun `parse Rose bot style - addblock star spam star warn`() {
            val result = AddBlockParser.parse("*spam* {warn}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("*spam*", result.parsed.pattern)
            assertEquals(PunishmentType.WARN, result.parsed.action)
        }

        @Test
        fun `parse Rose bot style - addblock badword mute 1h`() {
            val result = AddBlockParser.parse("badword {mute 1h}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("badword", result.parsed.pattern)
            assertEquals(PunishmentType.MUTE, result.parsed.action)
            assertEquals(60, result.parsed.durationMinutes)
        }

        @Test
        fun `parse Rose bot style - addblock scam ban`() {
            val result = AddBlockParser.parse("scam {ban}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("scam", result.parsed.pattern)
            assertEquals(PunishmentType.BAN, result.parsed.action)
        }

        @Test
        fun `parse complex wildcard pattern`() {
            val result = AddBlockParser.parse("*free*money* {ban 7d}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("*free*money*", result.parsed.pattern)
            assertEquals(PunishmentType.BAN, result.parsed.action)
            assertEquals(10080, result.parsed.durationMinutes)  // 7 * 24 * 60
        }

        @Test
        fun `parse URL-like pattern`() {
            val result = AddBlockParser.parse("*t.me/joinchat* {ban}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("*t.me/joinchat*", result.parsed.pattern)
            assertEquals(PunishmentType.BAN, result.parsed.action)
        }
    }

    @Nested
    inner class EmojiPatternTests {

        @Test
        fun `parse single emoji pattern`() {
            val result = AddBlockParser.parse("🔥")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("🔥", result.parsed.pattern)
            assertNull(result.parsed.action)
        }

        @Test
        fun `parse emoji with action`() {
            val result = AddBlockParser.parse("🔥 {warn}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("🔥", result.parsed.pattern)
            assertEquals(PunishmentType.WARN, result.parsed.action)
        }

        @Test
        fun `parse emoji with wildcards`() {
            val result = AddBlockParser.parse("*🔥*")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("*🔥*", result.parsed.pattern)
        }

        @Test
        fun `parse composite emoji (flag)`() {
            val result = AddBlockParser.parse("🇷🇺 {ban}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("🇷🇺", result.parsed.pattern)
            assertEquals(PunishmentType.BAN, result.parsed.action)
        }

        @Test
        fun `parse multiple emojis pattern`() {
            val result = AddBlockParser.parse("🔥👍🎉 {warn}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("🔥👍🎉", result.parsed.pattern)
            assertEquals(PunishmentType.WARN, result.parsed.action)
        }

        @Test
        fun `parse text with emoji`() {
            val result = AddBlockParser.parse("spam🔥 {mute 1h}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("spam🔥", result.parsed.pattern)
            assertEquals(PunishmentType.MUTE, result.parsed.action)
            assertEquals(60, result.parsed.durationMinutes)
        }

        @Test
        fun `parse emoji with wildcard pattern`() {
            val result = AddBlockParser.parse("*🔥* {ban}")

            assertIs<AddBlockParser.Result.Success>(result)
            assertEquals("*🔥*", result.parsed.pattern)
            assertEquals(PunishmentType.BAN, result.parsed.action)
        }
    }
}
