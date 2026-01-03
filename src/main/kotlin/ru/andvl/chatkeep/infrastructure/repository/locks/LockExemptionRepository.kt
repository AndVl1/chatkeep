package ru.andvl.chatkeep.infrastructure.repository.locks

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.andvl.chatkeep.domain.model.locks.LockExemption

@Repository
interface LockExemptionRepository : CrudRepository<LockExemption, Long> {
    fun findAllByChatId(chatId: Long): List<LockExemption>
    fun findAllByChatIdAndLockType(chatId: Long, lockType: String?): List<LockExemption>
    fun deleteByChatIdAndExemptionTypeAndExemptionValue(chatId: Long, exemptionType: String, exemptionValue: String)
}
