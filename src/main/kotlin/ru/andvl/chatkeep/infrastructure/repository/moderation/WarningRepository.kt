package ru.andvl.chatkeep.infrastructure.repository.moderation

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.moderation.Warning
import java.time.Instant

interface WarningRepository : CrudRepository<Warning, Long> {

    @Query("""
        SELECT COUNT(*) FROM warnings
        WHERE chat_id = :chatId
        AND user_id = :userId
        AND expires_at > :now
    """)
    fun countActiveByChatIdAndUserId(chatId: Long, userId: Long, now: Instant): Int

    @Query("""
        SELECT * FROM warnings
        WHERE chat_id = :chatId
        AND user_id = :userId
        AND expires_at > :now
        ORDER BY created_at DESC
    """)
    fun findActiveByChatIdAndUserId(chatId: Long, userId: Long, now: Instant): List<Warning>

    @Modifying
    @Query("""
        DELETE FROM warnings
        WHERE chat_id = :chatId
        AND user_id = :userId
        AND expires_at > :now
    """)
    fun deleteActiveByChatIdAndUserId(chatId: Long, userId: Long, now: Instant)
}
