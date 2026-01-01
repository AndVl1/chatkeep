package ru.andvl.chatkeep.infrastructure.repository.moderation

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.moderation.AdminCacheEntry
import java.time.Instant

interface AdminCacheRepository : CrudRepository<AdminCacheEntry, Long> {

    @Query("""
        SELECT * FROM admin_cache
        WHERE user_id = :userId
        AND chat_id = :chatId
        AND expires_at > :now
    """)
    fun findValidEntry(userId: Long, chatId: Long, now: Instant): AdminCacheEntry?

    @Modifying
    @Query("""
        INSERT INTO admin_cache (user_id, chat_id, is_admin, cached_at, expires_at)
        VALUES (:userId, :chatId, :isAdmin, :cachedAt, :expiresAt)
        ON CONFLICT (user_id, chat_id) DO UPDATE SET
            is_admin = EXCLUDED.is_admin,
            cached_at = EXCLUDED.cached_at,
            expires_at = EXCLUDED.expires_at
    """)
    fun upsert(userId: Long, chatId: Long, isAdmin: Boolean, cachedAt: Instant, expiresAt: Instant)

    @Modifying
    @Query("""
        DELETE FROM admin_cache
        WHERE user_id = :userId
        AND chat_id = :chatId
    """)
    fun deleteByUserIdAndChatId(userId: Long, chatId: Long)

    @Modifying
    @Query("DELETE FROM admin_cache WHERE expires_at <= :now")
    fun deleteExpired(now: Instant)
}
