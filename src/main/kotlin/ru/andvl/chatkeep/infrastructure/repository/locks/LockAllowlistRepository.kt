package ru.andvl.chatkeep.infrastructure.repository.locks

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.andvl.chatkeep.domain.model.locks.LockAllowlist

@Repository
interface LockAllowlistRepository : CrudRepository<LockAllowlist, Long> {
    fun findAllByChatId(chatId: Long): List<LockAllowlist>
    fun findAllByChatIdAndAllowlistType(chatId: Long, allowlistType: String): List<LockAllowlist>
    fun deleteByChatIdAndAllowlistTypeAndPattern(chatId: Long, allowlistType: String, pattern: String)
}
