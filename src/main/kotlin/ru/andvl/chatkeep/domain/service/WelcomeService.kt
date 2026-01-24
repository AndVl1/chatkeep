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
        logger.info("updateWelcomeSettings: chatId=$chatId, existingFound=${existing != null}")

        val updated = if (existing != null) {
            logger.info("Updating existing welcome settings for chatId=$chatId")
            existing.copy(
                enabled = settings.enabled,
                messageText = settings.messageText,
                sendToChat = settings.sendToChat,
                deleteAfterSeconds = settings.deleteAfterSeconds,
                updatedAt = Instant.now()
            )
        } else {
            logger.info("Creating new welcome settings for chatId=$chatId with isNew=true")
            WelcomeSettings.createNew(
                chatId = chatId,
                enabled = settings.enabled,
                messageText = settings.messageText,
                sendToChat = settings.sendToChat,
                deleteAfterSeconds = settings.deleteAfterSeconds
            )
        }

        logger.info("Saving welcome settings: isNew=${updated.isNew()}")
        val saved = repository.save(updated)
        logger.info("Successfully saved welcome settings for chatId=$chatId")
        return saved
    }
}
