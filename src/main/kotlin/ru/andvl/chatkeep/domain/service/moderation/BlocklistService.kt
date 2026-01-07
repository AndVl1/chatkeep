package ru.andvl.chatkeep.domain.service.moderation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.bot.util.RoseImportParser
import ru.andvl.chatkeep.domain.model.moderation.BlocklistPattern
import ru.andvl.chatkeep.domain.model.moderation.MatchType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.infrastructure.repository.moderation.BlocklistPatternRepository
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository

@Service
class BlocklistService(
    private val blocklistPatternRepository: BlocklistPatternRepository,
    private val moderationConfigRepository: ModerationConfigRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    data class BlocklistMatch(
        val pattern: BlocklistPattern,
        val action: PunishmentType,
        val durationMinutes: Int?
    )

    data class AddPatternResult(
        val pattern: BlocklistPattern,
        val isUpdate: Boolean
    )

    data class ImportResult(
        val added: Int,
        val updated: Int,
        val skipped: Int
    )

    fun checkMessage(chatId: Long, text: String): BlocklistMatch? {
        val patterns = blocklistPatternRepository.findByChatIdOrGlobal(chatId)
        val normalizedText = text.lowercase()

        // Find all matches and return highest severity
        val matches = patterns.mapNotNull { pattern ->
            if (matchesPattern(normalizedText, pattern)) {
                BlocklistMatch(
                    pattern = pattern,
                    action = PunishmentType.valueOf(pattern.action),
                    durationMinutes = pattern.actionDurationMinutes
                )
            } else {
                null
            }
        }

        // Return highest severity match (patterns already ordered by severity DESC)
        val match = matches.firstOrNull()
        if (match != null) {
            logger.info("Message matched blocklist: pattern='${match.pattern.pattern}', action=${match.action}")
        }

        return match
    }

    private fun matchesPattern(text: String, pattern: BlocklistPattern): Boolean {
        val patternText = pattern.pattern.lowercase()

        return when (MatchType.valueOf(pattern.matchType)) {
            MatchType.EXACT -> text.contains(patternText)
            MatchType.WILDCARD -> {
                // Safety check: limit wildcards to prevent ReDoS
                val wildcardCount = patternText.count { it == '*' || it == '?' }
                if (wildcardCount > 5) {
                    logger.warn("Pattern has too many wildcards, skipping: $patternText")
                    return false
                }

                // Build regex by escaping non-wildcard parts and converting wildcards
                // Use codePoints() to correctly handle emojis (multi-byte Unicode characters)
                val regexPattern = buildString {
                    patternText.codePoints().forEach { codePoint ->
                        val char = codePoint.toChar()
                        when {
                            codePoint == '*'.code -> append(".*")
                            codePoint == '?'.code -> append(".")
                            else -> {
                                // For multi-byte characters (emojis), convert codepoint to string
                                val str = String(Character.toChars(codePoint))
                                // Escape regex special characters (only applies to single-byte chars)
                                if (str.length == 1 && str[0] in """\.[]{}()|^$+""") {
                                    append('\\')
                                }
                                append(str)
                            }
                        }
                    }
                }

                try {
                    Regex(regexPattern).containsMatchIn(text)
                } catch (e: Exception) {
                    logger.warn("Invalid pattern: $patternText", e)
                    false
                }
            }
        }
    }

    fun addPattern(
        chatId: Long?,
        pattern: String,
        matchType: MatchType,
        action: PunishmentType,
        durationMinutes: Int?,
        severity: Int
    ): AddPatternResult {
        // Check if pattern already exists for this chat - update instead of creating duplicate
        val existing = chatId?.let { blocklistPatternRepository.findByChatIdAndPattern(it, pattern) }

        val blocklistPattern = if (existing != null) {
            // Update existing pattern
            existing.copy(
                matchType = matchType.name,
                action = action.name,
                actionDurationMinutes = durationMinutes,
                severity = severity
            )
        } else {
            // Create new pattern
            BlocklistPattern(
                chatId = chatId,
                pattern = pattern,
                matchType = matchType.name,
                action = action.name,
                actionDurationMinutes = durationMinutes,
                severity = severity
            )
        }

        val saved = blocklistPatternRepository.save(blocklistPattern)
        val isUpdate = existing != null
        val operation = if (isUpdate) "Updated" else "Added"
        logger.info("$operation blocklist pattern: chatId=$chatId, pattern='$pattern', action=$action")
        return AddPatternResult(saved, isUpdate)
    }

    fun removePattern(chatId: Long, pattern: String) {
        blocklistPatternRepository.deleteByChatIdAndPattern(chatId, pattern)
        logger.info("Removed blocklist pattern: chatId=$chatId, pattern='$pattern'")
    }

    fun deletePatternById(patternId: Long): Boolean {
        return try {
            blocklistPatternRepository.deleteById(patternId)
            logger.info("Deleted blocklist pattern by ID: id=$patternId")
            true
        } catch (e: Exception) {
            logger.warn("Failed to delete blocklist pattern by ID: id=$patternId", e)
            false
        }
    }

    fun listPatterns(chatId: Long): List<BlocklistPattern> {
        return blocklistPatternRepository.findByChatId(chatId)
    }

    fun listGlobalPatterns(): List<BlocklistPattern> {
        return blocklistPatternRepository.findGlobalPatterns()
    }

    fun getDefaultAction(chatId: Long): PunishmentType {
        val config = moderationConfigRepository.findByChatId(chatId)
        val actionName = config?.defaultBlocklistAction ?: "WARN"
        return try {
            PunishmentType.valueOf(actionName)
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid default action '$actionName' for chat $chatId, falling back to WARN")
            PunishmentType.WARN
        }
    }

    fun importPatterns(chatId: Long, items: List<RoseImportParser.ImportItem>): ImportResult {
        var added = 0
        var updated = 0

        items.forEach { item ->
            val result = addPattern(
                chatId = chatId,
                pattern = item.pattern,
                matchType = item.matchType,
                action = item.action,
                durationMinutes = item.durationMinutes,
                severity = item.severity
            )

            if (result.isUpdate) {
                updated++
            } else {
                added++
            }
        }

        logger.info("Imported patterns: chatId=$chatId, added=$added, updated=$updated")
        return ImportResult(added, updated, 0)
    }
}
