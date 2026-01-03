package ru.andvl.chatkeep.domain.service.channelreply

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class MediaGroupCacheService {

    private val cache = ConcurrentHashMap<String, Instant>()

    companion object {
        private const val TTL_MINUTES = 5L
    }

    fun isProcessed(mediaGroupId: String): Boolean {
        cleanExpired()
        return cache.containsKey(mediaGroupId)
    }

    fun markProcessed(mediaGroupId: String) {
        cache[mediaGroupId] = Instant.now()
    }

    private fun cleanExpired() {
        val now = Instant.now()
        val expirationThreshold = now.minusSeconds(TTL_MINUTES * 60)

        cache.entries.removeIf { entry ->
            entry.value.isBefore(expirationThreshold)
        }
    }
}
