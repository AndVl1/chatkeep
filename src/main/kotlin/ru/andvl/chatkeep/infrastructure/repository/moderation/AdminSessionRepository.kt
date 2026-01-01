package ru.andvl.chatkeep.infrastructure.repository.moderation

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.moderation.AdminSession

interface AdminSessionRepository : CrudRepository<AdminSession, Long> {

    @Query("SELECT * FROM admin_sessions WHERE user_id = :userId")
    fun findByUserId(userId: Long): AdminSession?

    @Query("DELETE FROM admin_sessions WHERE user_id = :userId")
    fun deleteByUserId(userId: Long)
}
