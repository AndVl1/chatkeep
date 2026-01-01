package ru.andvl.chatkeep.domain.service.logchannel.dto

import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import java.time.Instant
import kotlin.time.Duration

/**
 * Data transfer object for moderation log entries.
 * Contains all information needed to format and send a log message to a channel.
 */
data class ModerationLogEntry(
    val chatId: Long,
    val chatTitle: String?,
    val userId: Long,
    val userFirstName: String?,
    val userLastName: String?,
    val userName: String?,
    val adminId: Long,
    val adminFirstName: String?,
    val adminLastName: String?,
    val adminUserName: String?,
    val actionType: ActionType,
    val duration: Duration? = null,
    val reason: String? = null,
    val messageText: String? = null,
    val source: PunishmentSource,
    val timestamp: Instant = Instant.now()
) {
    /**
     * Format user mention for display.
     * Returns username if available, otherwise first name with optional last name.
     */
    fun formatUserMention(): String {
        val name = buildString {
            append(userFirstName ?: "User")
            userLastName?.let { append(" $it") }
        }
        return if (userName != null) {
            "<a href=\"tg://user?id=$userId\">$name</a> (@$userName)"
        } else {
            "<a href=\"tg://user?id=$userId\">$name</a>"
        }
    }

    /**
     * Format admin mention for display.
     */
    fun formatAdminMention(): String {
        val name = buildString {
            append(adminFirstName ?: "Admin")
            adminLastName?.let { append(" $it") }
        }
        return if (adminUserName != null) {
            "<a href=\"tg://user?id=$adminId\">$name</a> (@$adminUserName)"
        } else {
            "<a href=\"tg://user?id=$adminId\">$name</a>"
        }
    }

    /**
     * Format duration for display.
     */
    fun formatDuration(): String? {
        if (duration == null) return null

        val hours = duration.inWholeHours
        val days = hours / 24

        return when {
            days > 0 -> "${days}d ${hours % 24}h"
            hours > 0 -> "${hours}h"
            else -> "${duration.inWholeMinutes}m"
        }
    }
}
