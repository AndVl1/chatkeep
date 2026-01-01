package ru.andvl.chatkeep.domain.model.moderation

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("admin_cache")
data class AdminCacheEntry(
    @Id
    val id: Long? = null,

    @Column("user_id")
    val userId: Long,

    @Column("chat_id")
    val chatId: Long,

    @Column("is_admin")
    val isAdmin: Boolean,

    @Column("cached_at")
    val cachedAt: Instant = Instant.now(),

    @Column("expires_at")
    val expiresAt: Instant
)
