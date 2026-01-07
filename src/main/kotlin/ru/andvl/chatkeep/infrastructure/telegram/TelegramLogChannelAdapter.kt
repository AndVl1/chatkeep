package ru.andvl.chatkeep.infrastructure.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.LinkPreviewOptions
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelPort
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry

/**
 * Telegram implementation of LogChannelPort.
 * Sends formatted HTML messages to Telegram channels.
 */
@Component
class TelegramLogChannelAdapter(
    private val bot: TelegramBot
) : LogChannelPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun sendLogEntry(logChannelId: Long, entry: ModerationLogEntry): Boolean {
        return try {
            val message = formatLogMessage(entry)
            val chatId = ChatId(RawChatId(logChannelId))

            bot.sendTextMessage(
                chatId = chatId,
                text = message,
                parseMode = HTMLParseMode,
                linkPreviewOptions = LinkPreviewOptions.Disabled
            )

            logger.debug("Sent log entry to channel $logChannelId: ${entry.actionType}")
            true
        } catch (e: Exception) {
            logger.error("Failed to send log entry to channel $logChannelId: ${e.message}", e)
            false
        }
    }

    override suspend fun validateChannel(channelId: Long): Boolean {
        return try {
            val chatId = ChatId(RawChatId(channelId))
            bot.getChat(chatId)
            true
        } catch (e: Exception) {
            logger.warn("Cannot access channel $channelId: ${e.message}")
            false
        }
    }

    /**
     * Format the log entry as an HTML message following Rose Bot style.
     */
    private fun formatLogMessage(entry: ModerationLogEntry): String {
        return buildString {
            // Chat title
            entry.chatTitle?.let { appendLine("<b>Chat:</b> $it") }

            // Action hashtag
            val hashtag = getActionHashtag(entry.actionType)
            appendLine("<b>$hashtag</b>")
            appendLine()

            // Admin info
            appendLine("<b>Admin:</b> ${entry.formatAdminMention()}")

            // User info (only for user-targeted actions)
            if (entry.userId != null) {
                appendLine("<b>User:</b> ${entry.formatUserMention()}")
            }

            // Duration (for mute/ban)
            if (entry.actionType in listOf(ActionType.MUTE, ActionType.BAN)) {
                entry.formatDuration()?.let { duration ->
                    appendLine("<b>Duration:</b> $duration")
                }
            }

            // Reason (skip for config-only actions, but show for lock enabled/disabled as it contains lock type)
            val configActions = listOf(
                ActionType.CLEAN_SERVICE_ON,
                ActionType.CLEAN_SERVICE_OFF,
                ActionType.LOCK_WARNS_ON,
                ActionType.LOCK_WARNS_OFF,
                ActionType.CONFIG_CHANGED,
                ActionType.LOCK_ENABLED,
                ActionType.LOCK_DISABLED
            )
            if (entry.actionType !in configActions) {
                val reason = entry.reason ?: "No reason provided"
                appendLine("<b>Reason:</b> $reason")
            }

            // Lock type for lock enabled/disabled
            if (entry.actionType in listOf(ActionType.LOCK_ENABLED, ActionType.LOCK_DISABLED)) {
                entry.reason?.let { lockType ->
                    appendLine("<b>Lock Type:</b> $lockType")
                }
            }

            // Config change details
            if (entry.actionType == ActionType.CONFIG_CHANGED && entry.reason != null) {
                appendLine("<b>Changes:</b> ${entry.reason}")
            }

            // Source (if not manual)
            if (entry.source != PunishmentSource.MANUAL) {
                appendLine("<b>Source:</b> ${entry.source.name.lowercase().replaceFirstChar { it.uppercase() }}")
            }

            // Quoted message (if available)
            entry.messageText?.let { text ->
                appendLine()
                appendLine("<b>Quoted message:</b>")
                // Escape HTML and truncate if too long
                val escapedText = escapeHtml(text).take(500)
                val truncated = if (text.length > 500) "$escapedText..." else escapedText
                appendLine("<i>$truncated</i>")
            }
        }
    }

    /**
     * Get hashtag for action type (like Rose Bot).
     */
    private fun getActionHashtag(actionType: ActionType): String {
        return when (actionType) {
            ActionType.NOTHING -> "#NOTHING"
            ActionType.WARN -> "#WARNED"
            ActionType.UNWARN -> "#UNWARNED"
            ActionType.MUTE -> "#MUTED"
            ActionType.UNMUTE -> "#UNMUTED"
            ActionType.BAN -> "#BANNED"
            ActionType.UNBAN -> "#UNBANNED"
            ActionType.KICK -> "#KICKED"
            ActionType.CLEAN_SERVICE_ON -> "#CLEANSERVICE_ON"
            ActionType.CLEAN_SERVICE_OFF -> "#CLEANSERVICE_OFF"
            ActionType.LOCK_WARNS_ON -> "#LOCKWARNS_ON"
            ActionType.LOCK_WARNS_OFF -> "#LOCKWARNS_OFF"
            ActionType.LOCK_ENABLED -> "#LOCK_ENABLED"
            ActionType.LOCK_DISABLED -> "#LOCK_DISABLED"
            ActionType.CONFIG_CHANGED -> "#CONFIG_CHANGED"
            ActionType.BLOCKLIST_REMOVED -> "#BLOCKLIST_REMOVED"
        }
    }

    /**
     * Escape special HTML characters.
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
