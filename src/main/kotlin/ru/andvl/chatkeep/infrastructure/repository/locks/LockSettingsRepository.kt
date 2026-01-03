package ru.andvl.chatkeep.infrastructure.repository.locks

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.andvl.chatkeep.domain.model.locks.LockSettings

@Repository
interface LockSettingsRepository : CrudRepository<LockSettings, Long> {
    fun findByChatId(chatId: Long): LockSettings?
}
