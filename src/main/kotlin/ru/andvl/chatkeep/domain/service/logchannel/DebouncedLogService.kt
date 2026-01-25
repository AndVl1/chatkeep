package ru.andvl.chatkeep.domain.service.logchannel

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for debouncing log entries to prevent spam when settings are synced in real-time.
 *
 * When a user types in a text field (e.g., welcome message, rules, Twitch templates),
 * changes can be synced every keystroke. This service coalesces rapid changes into a single
 * log entry that is sent after a configurable delay (default 15 seconds) after the last change.
 *
 * Non-debounced actions (toggles, discrete settings) are sent immediately via LogChannelService.
 */
@Service
class DebouncedLogService(
    private val logChannelService: LogChannelService,
    @Value("\${chatkeep.logging.debounce-delay-ms:15000}") private val debounceDelayMs: Long
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Scope for debounce timers
    private val debounceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Key for debounce tracking: chatId + actionType combination
     */
    private data class DebounceKey(val chatId: Long, val actionType: ActionType)

    /**
     * Pending entry with its timer job
     */
    private data class PendingEntry(
        val entry: ModerationLogEntry,
        val job: Job,
        val lastUpdate: Instant
    )

    // Map of pending entries waiting to be sent
    private val pendingEntries = ConcurrentHashMap<DebounceKey, PendingEntry>()

    // Action types that should be debounced (text-based settings that sync in real-time)
    private val debouncedActionTypes = setOf(
        ActionType.WELCOME_CHANGED,
        ActionType.RULES_CHANGED,
        ActionType.TWITCH_SETTINGS_CHANGED
    )

    /**
     * Log a moderation action with optional debouncing.
     *
     * For debounced action types:
     * - Cancels any pending timer for the same chat+action combination
     * - Starts a new timer that will send the log entry after the debounce delay
     * - If another update comes in before the timer fires, the timer is reset
     *
     * For non-debounced action types:
     * - Sends immediately via LogChannelService
     *
     * @param entry The moderation log entry to send
     */
    fun logAction(entry: ModerationLogEntry) {
        if (entry.actionType in debouncedActionTypes) {
            scheduleDebounced(entry)
        } else {
            // Non-debounced actions are sent immediately
            logChannelService.logModerationAction(entry)
        }
    }

    /**
     * Schedule a debounced log entry.
     * If there's already a pending entry for the same chat+action, it's replaced.
     */
    private fun scheduleDebounced(entry: ModerationLogEntry) {
        val key = DebounceKey(entry.chatId, entry.actionType)

        // Cancel any existing timer for this key
        pendingEntries[key]?.job?.cancel()

        // Create new timer
        val job = debounceScope.launch {
            delay(debounceDelayMs)

            // Remove from pending and send
            pendingEntries.remove(key)?.let { pending ->
                logger.debug(
                    "Debounce timer fired for chat {} action {}, sending log entry",
                    entry.chatId, entry.actionType
                )
                logChannelService.logModerationAction(pending.entry)
            }
        }

        // Store pending entry
        pendingEntries[key] = PendingEntry(
            entry = entry,
            job = job,
            lastUpdate = Instant.now()
        )

        logger.debug(
            "Scheduled debounced log for chat {} action {} (delay: {}ms)",
            entry.chatId, entry.actionType, debounceDelayMs
        )
    }

    /**
     * Flush all pending entries immediately.
     * Useful for graceful shutdown or testing.
     */
    fun flushAll() {
        pendingEntries.forEach { (key, pending) ->
            pending.job.cancel()
            logChannelService.logModerationAction(pending.entry)
        }
        pendingEntries.clear()
    }

    /**
     * Flush pending entries for a specific chat.
     * Useful when a chat's log channel is being changed.
     */
    fun flushForChat(chatId: Long) {
        pendingEntries.entries
            .filter { it.key.chatId == chatId }
            .forEach { (key, pending) ->
                pending.job.cancel()
                logChannelService.logModerationAction(pending.entry)
                pendingEntries.remove(key)
            }
    }

    /**
     * Cancel all pending entries without sending.
     * Useful for testing or when log channel is removed.
     */
    fun cancelAll() {
        pendingEntries.forEach { (_, pending) ->
            pending.job.cancel()
        }
        pendingEntries.clear()
    }
}
