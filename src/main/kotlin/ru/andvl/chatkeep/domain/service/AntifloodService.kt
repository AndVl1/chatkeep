package ru.andvl.chatkeep.domain.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.AntifloodSettings
import ru.andvl.chatkeep.infrastructure.repository.AntifloodSettingsRepository
import java.time.Duration
import java.time.Instant

@Service
class AntifloodService(
    private val repository: AntifloodSettingsRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // In-memory message tracking with time-based eviction to prevent memory leaks
    // Outer cache: chatId -> inner cache (evicts after 1 hour of inactivity, max 10k chats)
    // Inner cache: userId -> list of timestamps (evicts after 5 minutes of inactivity, max 1k users per chat)
    private val messageTimestamps: LoadingCache<Long, LoadingCache<Long, MutableList<Instant>>> =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(10000)
            .build { _ ->
                Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofMinutes(5))
                    .maximumSize(1000)
                    .build<Long, MutableList<Instant>> { mutableListOf() }
            }

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
            AntifloodSettings.createNew(
                chatId = chatId,
                enabled = settings.enabled,
                maxMessages = settings.maxMessages,
                timeWindowSeconds = settings.timeWindowSeconds,
                action = settings.action,
                actionDurationMinutes = settings.actionDurationMinutes
            )
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

        // Get or create chat message cache
        val chatCache = messageTimestamps.get(chatId)

        // Get or create user message list
        val userMessages = chatCache.get(userId)

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
        messageTimestamps.getIfPresent(chatId)?.invalidate(userId)
    }

    /**
     * Clear all flood tracking for a chat.
     */
    fun clearChatTracking(chatId: Long) {
        messageTimestamps.invalidate(chatId)
    }
}
