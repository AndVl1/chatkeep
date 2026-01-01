package ru.andvl.chatkeep.domain.model.moderation

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * Cache entry mapping Telegram username to user ID.
 * Populated from group messages to enable @username resolution.
 */
@Table("username_cache")
data class UsernameCacheEntry(
    @Id val id: Long? = null,
    val username: String,
    val userId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val updatedAt: Instant = Instant.now()
)
