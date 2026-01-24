package ru.andvl.chatkeep.domain.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.WelcomeSettings
import ru.andvl.chatkeep.infrastructure.repository.WelcomeSettingsRepository
import java.time.Instant

@Service
class WelcomeService(
    private val repository: WelcomeSettingsRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getWelcomeSettings(chatId: Long): WelcomeSettings? {
        return repository.findByChatId(chatId)
    }

    @Transactional
    fun updateWelcomeSettings(chatId: Long, settings: WelcomeSettings): WelcomeSettings {
        val existing = repository.findByChatId(chatId)

        val updated = if (existing != null) {
            settings.copy(
                chatId = chatId,
                createdAt = existing.createdAt,
                updatedAt = Instant.now()
            )
        } else {
            WelcomeSettings.createNew(
                chatId = chatId,
                enabled = settings.enabled,
                messageText = settings.messageText,
                sendToChat = settings.sendToChat,
                deleteAfterSeconds = settings.deleteAfterSeconds
            )
        }

        val saved = repository.save(updated)
        logger.info("Updated welcome settings for chatId=$chatId")
        return saved
    }
}
