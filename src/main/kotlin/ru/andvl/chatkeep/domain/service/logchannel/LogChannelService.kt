package ru.andvl.chatkeep.domain.service.logchannel

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository

/**
 * Service for orchestrating moderation action logging to external channels.
 *
 * This service:
 * - Retrieves log channel configuration per chat
 * - Sends log entries asynchronously (non-blocking)
 * - Handles configuration management (set/unset log channel)
 */
@Service
class LogChannelService(
    private val logChannelPort: LogChannelPort,
    private val moderationConfigRepository: ModerationConfigRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        logger.error("Uncaught exception in log channel coroutine", exception)
    }

    // Use a separate scope for async log sending to avoid blocking moderation actions
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    /**
     * Log a moderation action to the configured log channel.
     * This method is non-blocking - it launches the send operation asynchronously.
     *
     * @param entry The moderation log entry to send
     */
    fun logModerationAction(entry: ModerationLogEntry) {
        val config = moderationConfigRepository.findByChatId(entry.chatId)
        val logChannelId = config?.logChannelId

        if (logChannelId == null) {
            logger.debug("No log channel configured for chat ${entry.chatId}, skipping log")
            return
        }

        // Send asynchronously to not block moderation actions
        logScope.launch {
            try {
                val success = logChannelPort.sendLogEntry(logChannelId, entry)
                if (!success) {
                    logger.warn("Failed to send log entry for chat ${entry.chatId} to channel $logChannelId")
                }
            } catch (e: Exception) {
                logger.error("Error sending log entry for chat ${entry.chatId}: ${e.message}", e)
            }
        }
    }

    /**
     * Set the log channel for a chat.
     *
     * @param chatId The chat to configure
     * @param logChannelId The target log channel
     * @return true if successful
     */
    fun setLogChannel(chatId: Long, logChannelId: Long): Boolean {
        val existingConfig = moderationConfigRepository.findByChatId(chatId)

        val updatedConfig = if (existingConfig != null) {
            existingConfig.copy(logChannelId = logChannelId)
        } else {
            ModerationConfig(chatId = chatId, logChannelId = logChannelId)
        }

        moderationConfigRepository.save(updatedConfig)
        logger.info("Set log channel for chat $chatId to $logChannelId")
        return true
    }

    /**
     * Remove the log channel configuration for a chat.
     *
     * @param chatId The chat to unconfigure
     * @return true if there was a configuration to remove
     */
    fun unsetLogChannel(chatId: Long): Boolean {
        val existingConfig = moderationConfigRepository.findByChatId(chatId)

        if (existingConfig == null || existingConfig.logChannelId == null) {
            logger.debug("No log channel configured for chat $chatId")
            return false
        }

        val updatedConfig = existingConfig.copy(logChannelId = null)
        moderationConfigRepository.save(updatedConfig)
        logger.info("Removed log channel for chat $chatId")
        return true
    }

    /**
     * Get the current log channel for a chat.
     *
     * @param chatId The chat to check
     * @return The log channel ID or null if not configured
     */
    fun getLogChannel(chatId: Long): Long? {
        return moderationConfigRepository.findByChatId(chatId)?.logChannelId
    }

    /**
     * Validate that the bot can access the specified channel.
     *
     * @param channelId The channel to validate
     * @return true if the bot can send to this channel
     */
    suspend fun validateChannel(channelId: Long): Boolean {
        return logChannelPort.validateChannel(channelId)
    }
}
