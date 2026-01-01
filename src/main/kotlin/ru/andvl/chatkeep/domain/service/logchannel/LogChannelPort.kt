package ru.andvl.chatkeep.domain.service.logchannel

import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry

/**
 * Port interface for sending moderation log entries to external channels.
 *
 * This abstraction allows for different implementations:
 * - Telegram channels (primary)
 * - Discord webhooks (future)
 * - Slack channels (future)
 * - HTTP webhooks (future)
 *
 * Follows the Ports & Adapters (Hexagonal) architecture pattern.
 */
interface LogChannelPort {

    /**
     * Send a moderation log entry to the specified channel.
     *
     * @param logChannelId The target channel identifier
     * @param entry The moderation log entry to send
     * @return true if the message was sent successfully, false otherwise
     */
    suspend fun sendLogEntry(logChannelId: Long, entry: ModerationLogEntry): Boolean

    /**
     * Validate that the bot has access to the specified channel.
     *
     * @param channelId The channel to validate
     * @return true if the bot can send messages to this channel
     */
    suspend fun validateChannel(channelId: Long): Boolean
}
