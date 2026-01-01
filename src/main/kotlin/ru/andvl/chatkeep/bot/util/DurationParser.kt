package ru.andvl.chatkeep.bot.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object DurationParser {

    private const val MAX_DAYS = 999L
    private const val MAX_HOURS = 9999L
    private const val MAX_MINUTES = 599940L  // 9999 hours

    fun parse(input: String): Duration? {
        val regex = Regex("""(\d+)([mhd])""")
        val match = regex.matchEntire(input.lowercase()) ?: return null

        val value = match.groupValues[1].toLongOrNull() ?: return null
        val unit = match.groupValues[2]

        return when (unit) {
            "m" -> if (value <= MAX_MINUTES) value.minutes else null
            "h" -> if (value <= MAX_HOURS) value.hours else null
            "d" -> if (value <= MAX_DAYS) value.days else null
            else -> null
        }
    }

    fun toSeconds(duration: Duration): Long = duration.inWholeSeconds

    fun toHours(duration: Duration): Int {
        val hours = duration.inWholeHours
        return when {
            hours > Int.MAX_VALUE -> Int.MAX_VALUE
            hours < 0 -> 0
            else -> hours.toInt()
        }
    }
}
