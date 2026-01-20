package ru.andvl.chatkeep.domain.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.AntifloodSettings
import ru.andvl.chatkeep.infrastructure.repository.AntifloodSettingsRepository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class AntifloodService(
    private val repository: AntifloodSettingsRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // In-memory message tracking: chatId -> userId -> list of timestamps
    private val messageTimestamps = ConcurrentHashMap<Long, ConcurrentHashMap<Long, MutableList<Instant>>>()

    fun getSettings(chatId: Long): AntifloodSettings? {
        return repository.findByChatId(chatId)
    }

    @Transactional
    fun updateSettings(chatId: Long, settings: AntifloodSettings): AntifloodSettings {
        val existing = repository.findByChatId(chatId)

        val updated = if (existing != null) {
            settings.copy(
                chatId = chatId,
                createdAt = existing.createdAt,
                updatedAt = Instant.now()
            )
        } else {
            settings.copy(chatId = chatId)
        }

        val saved = repository.save(updated)
        logger.info("Updated antiflood settings for chatId=$chatId")
        return saved
    }

    /**
     * Check if user is flooding and should be punished.
     * Returns true if flood threshold is exceeded.
     */
    fun checkFlood(chatId: Long, userId: Long): Boolean {
        val settings = getSettings(chatId) ?: return false
        if (!settings.enabled) return false

        val now = Instant.now()
        val windowStart = now.minusSeconds(settings.timeWindowSeconds.toLong())

        // Get or create chat message map
        val chatMap = messageTimestamps.getOrPut(chatId) { ConcurrentHashMap() }

        // Get or create user message list
        val userMessages = chatMap.getOrPut(userId) { mutableListOf() }

        // Remove old messages outside the window
        synchronized(userMessages) {
            userMessages.removeIf { it.isBefore(windowStart) }

            // Add current message
            userMessages.add(now)

            // Check threshold
            val isFlooding = userMessages.size > settings.maxMessages

            if (isFlooding) {
                logger.info("Flood detected: chatId=$chatId, userId=$userId, count=${userMessages.size}")
            }

            return isFlooding
        }
    }

    /**
     * Clear flood tracking for a user (called after punishment).
     */
    fun clearFloodTracking(chatId: Long, userId: Long) {
        messageTimestamps[chatId]?.remove(userId)
    }

    /**
     * Clear all flood tracking for a chat.
     */
    fun clearChatTracking(chatId: Long) {
        messageTimestamps.remove(chatId)
    }
}
