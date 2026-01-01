package ru.andvl.chatkeep.domain.service.moderation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.infrastructure.repository.moderation.UsernameCacheRepository
import java.time.Instant

/**
 * Service for caching username to user ID mappings.
 * Used to resolve @username mentions in moderation commands.
 */
@Service
class UsernameCacheService(
    private val repository: UsernameCacheRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Cache or update a username mapping.
     * Called when processing messages from groups.
     */
    @Transactional
    fun cacheUsername(username: String, userId: Long, firstName: String?, lastName: String?) {
        try {
            repository.upsert(
                username = username.lowercase(),
                userId = userId,
                firstName = firstName,
                lastName = lastName,
                updatedAt = Instant.now()
            )
        } catch (e: Exception) {
            logger.debug("Failed to cache username @$username: ${e.message}")
        }
    }

    /**
     * Resolve a username to user ID from cache.
     * @param username Username without @ prefix
     * @return User ID if found in cache, null otherwise
     */
    fun resolveUsername(username: String): Long? {
        return try {
            repository.findByUsernameIgnoreCase(username)?.userId
        } catch (e: Exception) {
            logger.debug("Failed to resolve username @$username: ${e.message}")
            null
        }
    }

    /**
     * Get cached user info by user ID.
     */
    fun getUserInfo(userId: Long): CachedUserInfo? {
        return try {
            repository.findByUserId(userId)?.let {
                CachedUserInfo(
                    userId = it.userId,
                    username = it.username,
                    firstName = it.firstName,
                    lastName = it.lastName
                )
            }
        } catch (e: Exception) {
            logger.debug("Failed to get user info for $userId: ${e.message}")
            null
        }
    }
}

data class CachedUserInfo(
    val userId: Long,
    val username: String,
    val firstName: String?,
    val lastName: String?
)
