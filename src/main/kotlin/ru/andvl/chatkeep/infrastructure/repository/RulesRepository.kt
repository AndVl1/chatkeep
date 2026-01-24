package ru.andvl.chatkeep.infrastructure.repository

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.Rules

interface RulesRepository : CrudRepository<Rules, Long> {

    @Query("SELECT * FROM rules WHERE chat_id = :chatId")
    fun findByChatId(chatId: Long): Rules?
}
