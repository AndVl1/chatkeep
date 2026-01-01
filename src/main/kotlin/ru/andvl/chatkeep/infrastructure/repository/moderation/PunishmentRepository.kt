package ru.andvl.chatkeep.infrastructure.repository.moderation

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.moderation.Punishment

interface PunishmentRepository : CrudRepository<Punishment, Long> {

    @Query("""
        SELECT * FROM punishments
        WHERE chat_id = :chatId
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun findByChatId(chatId: Long, limit: Int = 100): List<Punishment>

    @Query("""
        SELECT * FROM punishments
        WHERE user_id = :userId
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun findByUserId(userId: Long, limit: Int = 100): List<Punishment>

    @Query("""
        SELECT * FROM punishments
        WHERE chat_id = :chatId
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun findByChatIdOrderByCreatedAtDesc(chatId: Long, limit: Int = 10000): List<Punishment>
}
