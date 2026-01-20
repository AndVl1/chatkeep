package ru.andvl.chatkeep.infrastructure.repository

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.WelcomeSettings

interface WelcomeSettingsRepository : CrudRepository<WelcomeSettings, Long> {

    @Query("SELECT * FROM welcome_settings WHERE chat_id = :chatId")
    fun findByChatId(chatId: Long): WelcomeSettings?
}
