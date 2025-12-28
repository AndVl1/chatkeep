package ru.andvl.chatkeep.infrastructure.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.andvl.chatkeep.domain.model.ChatSettings

@Repository
interface ChatSettingsRepository : CrudRepository<ChatSettings, Long> {

    fun findByChatId(chatId: Long): ChatSettings?

    fun existsByChatId(chatId: Long): Boolean
}
