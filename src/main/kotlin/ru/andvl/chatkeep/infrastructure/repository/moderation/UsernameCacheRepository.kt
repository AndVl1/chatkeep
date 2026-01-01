package ru.andvl.chatkeep.infrastructure.repository.moderation

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.moderation.UsernameCacheEntry
import java.time.Instant

interface UsernameCacheRepository : CrudRepository<UsernameCacheEntry, Long> {

    @Query("""
        SELECT * FROM username_cache
        WHERE LOWER(username) = LOWER(:username)
    """)
    fun findByUsernameIgnoreCase(username: String): UsernameCacheEntry?

    @Query("""
        SELECT * FROM username_cache
        WHERE user_id = :userId
    """)
    fun findByUserId(userId: Long): UsernameCacheEntry?

    @Modifying
    @Query("""
        INSERT INTO username_cache (username, user_id, first_name, last_name, updated_at)
        VALUES (:username, :userId, :firstName, :lastName, :updatedAt)
        ON CONFLICT (username) DO UPDATE SET
            user_id = EXCLUDED.user_id,
            first_name = EXCLUDED.first_name,
            last_name = EXCLUDED.last_name,
            updated_at = EXCLUDED.updated_at
    """)
    fun upsert(username: String, userId: Long, firstName: String?, lastName: String?, updatedAt: Instant)
}
