package ru.andvl.chatkeep.domain.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.ChatSettings
import ru.andvl.chatkeep.infrastructure.repository.ChatSettingsRepository
import java.time.Instant

@Service
class ChatService(
    private val chatSettingsRepository: ChatSettingsRepository,
    private val metricsService: ru.andvl.chatkeep.metrics.BotMetricsService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun registerChat(chatId: Long, chatTitle: String?): ChatSettings {
        val existing = chatSettingsRepository.findByChatId(chatId)
        if (existing != null) {
            logger.debug("Chat $chatId already registered")
            return existing
        }

        val settings = ChatSettings(
            chatId = chatId,
            chatTitle = chatTitle,
            collectionEnabled = true
        )

        return chatSettingsRepository.save(settings).also {
            logger.info("Registered new chat: $chatId ($chatTitle)")
        }
    }

    fun getSettings(chatId: Long): ChatSettings? = chatSettingsRepository.findByChatId(chatId)

    fun isCollectionEnabled(chatId: Long): Boolean {
        val settings = chatSettingsRepository.findByChatId(chatId)
        return settings?.collectionEnabled ?: true
    }

    @Transactional
    fun setCollectionEnabled(chatId: Long, enabled: Boolean): ChatSettings? {
        val settings = chatSettingsRepository.findByChatId(chatId) ?: return null

        val updated = settings.copy(
            collectionEnabled = enabled,
            updatedAt = Instant.now()
        )

        return chatSettingsRepository.save(updated).also {
            logger.info("Chat $chatId collection ${if (enabled) "enabled" else "disabled"}")
            updateActiveChatsMetric()
        }
    }

    fun getAllChats(): List<ChatSettings> = chatSettingsRepository.findAll().toList()

    private fun updateActiveChatsMetric() {
        val activeCount = chatSettingsRepository.findAll()
            .count { it.collectionEnabled }
            .toLong()
        metricsService.setActiveChats(activeCount)
    }
}
