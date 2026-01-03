package ru.andvl.chatkeep.infrastructure.repository.locks

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.andvl.chatkeep.domain.model.locks.LockSettings
import java.util.Optional

@Repository
interface LockSettingsRepository : CrudRepository<LockSettings, Long> {
    // Note: chatId IS the @Id, so we use findById from CrudRepository
    // This custom method is kept for backward compatibility but delegates to findById
}

// Extension function to find by chatId (which is the @Id)
fun LockSettingsRepository.findByChatId(chatId: Long): LockSettings? =
    findById(chatId).orElse(null)
