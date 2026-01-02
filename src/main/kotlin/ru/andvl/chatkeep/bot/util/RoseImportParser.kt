package ru.andvl.chatkeep.bot.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import ru.andvl.chatkeep.domain.model.moderation.MatchType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType

object RoseImportParser {

    private const val MAX_PATTERN_LENGTH = 500
    private val objectMapper: ObjectMapper = ObjectMapper()

    data class ImportItem(
        val pattern: String,
        val action: PunishmentType,
        val durationHours: Int?,
        val severity: Int,
        val matchType: MatchType
    )

    data class ImportResult(
        val items: List<ImportItem>,
        val skippedCount: Int
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class RoseExport(
        val data: RoseData?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class RoseData(
        val blocklists: RoseBlocklists?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class RoseBlocklists(
        val filters: List<RoseFilter>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class RoseFilter(
        val name: String?,
        val reason: String?
    )

    fun parse(jsonContent: String): ImportResult {
        val items = mutableListOf<ImportItem>()
        var skipped = 0

        try {
            val export = objectMapper.readValue(jsonContent, RoseExport::class.java)
            val filters = export.data?.blocklists?.filters ?: emptyList()

            for (filter in filters) {
                val pattern = filter.name?.trim()
                if (pattern.isNullOrEmpty()) {
                    skipped++
                    continue
                }

                if (pattern.length > MAX_PATTERN_LENGTH) {
                    skipped++
                    continue
                }

                val reasonStr = filter.reason?.trim() ?: ""
                val (action, duration) = parseAction(reasonStr)
                val severity = calculateSeverity(action)
                val matchType = detectMatchType(pattern)

                items.add(
                    ImportItem(
                        pattern = pattern,
                        action = action,
                        durationHours = duration,
                        severity = severity,
                        matchType = matchType
                    )
                )
            }
        } catch (e: Exception) {
            // If parsing fails, return empty result with all skipped
            return ImportResult(emptyList(), skipped)
        }

        return ImportResult(items, skipped)
    }

    private fun parseAction(reason: String): Pair<PunishmentType, Int?> {
        if (reason.isEmpty()) {
            return PunishmentType.WARN to null
        }

        // Extract action from {action} or {action duration}
        val actionRegex = Regex("""\{([^}]+)\}""")
        val match = actionRegex.find(reason) ?: return PunishmentType.WARN to null

        val actionStr = match.groupValues[1].trim()
        val parts = actionStr.split(" ", limit = 2)
        val actionName = parts[0].lowercase()

        val action = when (actionName) {
            "ban", "sban" -> PunishmentType.BAN
            "kick" -> PunishmentType.KICK
            "warn" -> PunishmentType.WARN
            "mute", "tmute" -> PunishmentType.MUTE
            "nothing" -> PunishmentType.NOTHING
            else -> PunishmentType.WARN
        }

        // Parse duration for tmute
        val duration = if (actionName == "tmute" && parts.size == 2) {
            parseDuration(parts[1])
        } else {
            null
        }

        return action to duration
    }

    private fun parseDuration(durationStr: String): Int? {
        val duration = DurationParser.parse(durationStr) ?: return null
        return DurationParser.toHours(duration)
    }

    private fun calculateSeverity(action: PunishmentType): Int {
        return when (action) {
            PunishmentType.BAN -> 5
            PunishmentType.KICK -> 4
            PunishmentType.MUTE -> 3
            PunishmentType.WARN -> 2
            PunishmentType.NOTHING -> 1
        }
    }

    private fun detectMatchType(pattern: String): MatchType {
        return if (pattern.contains('*') || pattern.contains('?')) {
            MatchType.WILDCARD
        } else {
            MatchType.EXACT
        }
    }
}
