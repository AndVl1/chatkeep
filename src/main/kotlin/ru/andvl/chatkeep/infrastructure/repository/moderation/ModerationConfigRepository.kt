package ru.andvl.chatkeep.infrastructure.repository.moderation

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig

interface ModerationConfigRepository : CrudRepository<ModerationConfig, Long> {

    @Query("SELECT * FROM moderation_config WHERE chat_id = :chatId")
    fun findByChatId(chatId: Long): ModerationConfig?

    @Modifying
    @Query("UPDATE moderation_config SET clean_service_enabled = :enabled, updated_at = NOW() WHERE chat_id = :chatId")
    fun updateCleanServiceEnabled(chatId: Long, enabled: Boolean): Int
}
