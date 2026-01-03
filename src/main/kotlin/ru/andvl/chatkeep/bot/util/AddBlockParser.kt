package ru.andvl.chatkeep.bot.util

import ru.andvl.chatkeep.domain.model.moderation.PunishmentType

/**
 * Parser for /addblock command syntax.
 *
 * Syntax: /addblock <pattern> {action [duration]}
 *
 * Examples:
 * - /addblock spam                    -> pattern="spam", action=null (use default), duration=null
 * - /addblock *spam* {warn}           -> pattern="*spam*", action=WARN, duration=null
 * - /addblock badword {mute 1h}       -> pattern="badword", action=MUTE, duration=60
 * - /addblock scam {ban}              -> pattern="scam", action=BAN, duration=null
 * - /addblock test {mute 7d}          -> pattern="test", action=MUTE, duration=10080 (7*24*60)
 */
object AddBlockParser {

    private val BRACE_REGEX = Regex("""\{([^}]+)\}""")

    /**
     * Parsed result of /addblock command.
     */
    data class ParseResult(
        val pattern: String,
        val action: PunishmentType?,
        val durationMinutes: Int?
    )

    /**
     * Error during parsing.
     */
    sealed class ParseError {
        data object EmptyInput : ParseError()
        data object EmptyPattern : ParseError()
        data class PatternTooLong(val length: Int, val maxLength: Int = 100) : ParseError()
        data class UnknownAction(val actionStr: String) : ParseError()
    }

    /**
     * Parse result - either success or error.
     */
    sealed class Result {
        data class Success(val parsed: ParseResult) : Result()
        data class Failure(val error: ParseError) : Result()
    }

    /**
     * Parse the text after /addblock command.
     *
     * @param input The text after "/addblock " (trimmed)
     * @return Result with parsed data or error
     */
    fun parse(input: String): Result {
        val trimmed = input.trim()

        if (trimmed.isEmpty()) {
            return Result.Failure(ParseError.EmptyInput)
        }

        val braceMatch = BRACE_REGEX.find(trimmed)

        val pattern: String
        val action: PunishmentType?
        var durationMinutes: Int? = null

        if (braceMatch != null) {
            // Extract pattern (everything before the braces)
            pattern = trimmed.substring(0, braceMatch.range.first).trim()

            // Parse content inside braces
            val braceContent = braceMatch.groupValues[1].trim().split(Regex("\\s+"))
            val actionStr = braceContent.firstOrNull()?.uppercase() ?: ""

            action = try {
                PunishmentType.valueOf(actionStr)
            } catch (e: IllegalArgumentException) {
                return Result.Failure(ParseError.UnknownAction(actionStr))
            }

            // Parse optional duration
            if (braceContent.size > 1) {
                durationMinutes = DurationParser.parse(braceContent[1])?.let {
                    DurationParser.toMinutes(it)
                }
            }
        } else {
            // No braces - pattern only, action will use default
            pattern = trimmed
            action = null
        }

        // Validate pattern
        if (pattern.isEmpty()) {
            return Result.Failure(ParseError.EmptyPattern)
        }
        if (pattern.length > 100) {
            return Result.Failure(ParseError.PatternTooLong(pattern.length))
        }

        return Result.Success(ParseResult(pattern, action, durationMinutes))
    }
}
