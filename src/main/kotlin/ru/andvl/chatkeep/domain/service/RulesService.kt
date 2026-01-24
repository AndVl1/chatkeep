package ru.andvl.chatkeep.domain.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.Rules
import ru.andvl.chatkeep.infrastructure.repository.RulesRepository
import java.time.Instant

@Service
class RulesService(
    private val repository: RulesRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getRules(chatId: Long): Rules? {
        return repository.findByChatId(chatId)
    }

    @Transactional
    fun setRules(chatId: Long, rulesText: String): Rules {
        val existing = repository.findByChatId(chatId)

        val rules = if (existing != null) {
            existing.copy(
                rulesText = rulesText,
                updatedAt = Instant.now()
            )
        } else {
            Rules.createNew(
                chatId = chatId,
                rulesText = rulesText
            )
        }

        val saved = repository.save(rules)
        logger.info("Updated rules for chatId=$chatId")
        return saved
    }

    @Transactional
    fun deleteRules(chatId: Long) {
        repository.deleteById(chatId)
        logger.info("Deleted rules for chatId=$chatId")
    }
}
