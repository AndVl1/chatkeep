package ru.andvl.chatkeep.infrastructure.repository

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.AntifloodSettings

interface AntifloodSettingsRepository : CrudRepository<AntifloodSettings, Long> {

    @Query("SELECT * FROM antiflood_settings WHERE chat_id = :chatId")
    fun findByChatId(chatId: Long): AntifloodSettings?
}
