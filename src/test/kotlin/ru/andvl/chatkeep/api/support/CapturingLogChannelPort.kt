package ru.andvl.chatkeep.api.support

import kotlinx.coroutines.delay
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelPort
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Test implementation of LogChannelPort that captures log entries for verification.
 * Thread-safe for use in concurrent test scenarios.
 */
class CapturingLogChannelPort : LogChannelPort {

    private val capturedEntries = CopyOnWriteArrayList<Pair<Long, ModerationLogEntry>>()

    override suspend fun sendLogEntry(logChannelId: Long, entry: ModerationLogEntry): Boolean {
        capturedEntries.add(logChannelId to entry)
        return true
    }

    override suspend fun validateChannel(channelId: Long): Boolean {
        return true
    }

    /**
     * Get all captured log entries.
     */
    fun getCapturedEntries(): List<Pair<Long, ModerationLogEntry>> = capturedEntries.toList()

    /**
     * Get entries sent to a specific channel.
     */
    fun getEntriesForChannel(channelId: Long): List<ModerationLogEntry> =
        capturedEntries.filter { it.first == channelId }.map { it.second }

    /**
     * Verify that a log entry was sent with the expected properties.
     */
    fun verifySentLogEntry(
        channelId: Long,
        actionType: ActionType? = null,
        chatId: Long? = null
    ): Boolean {
        val entries = getEntriesForChannel(channelId)
        return entries.any { entry ->
            (actionType == null || entry.actionType == actionType) &&
                    (chatId == null || entry.chatId == chatId)
        }
    }

    /**
     * Verify that no log was sent to the specified channel.
     */
    fun verifyNoLogSent(channelId: Long): Boolean {
        return getEntriesForChannel(channelId).isEmpty()
    }

    /**
     * Wait for a log entry to arrive (useful for async operations).
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return The first matching entry, or null if timeout occurs
     */
    suspend fun waitForEntry(
        channelId: Long,
        actionType: ActionType? = null,
        timeoutMs: Long = 1000
    ): ModerationLogEntry? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val entries = getEntriesForChannel(channelId)
            val match = entries.firstOrNull { entry ->
                actionType == null || entry.actionType == actionType
            }
            if (match != null) return match
            delay(50)
        }
        return null
    }

    /**
     * Clear all captured entries (useful in test setup).
     */
    fun clear() {
        capturedEntries.clear()
    }

    /**
     * Get the count of entries sent to a channel.
     */
    fun getEntryCount(channelId: Long): Int = getEntriesForChannel(channelId).size
}
