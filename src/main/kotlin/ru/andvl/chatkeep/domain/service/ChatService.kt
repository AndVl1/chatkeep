package ru.andvl.chatkeep.domain.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.ChatSettings
import ru.andvl.chatkeep.domain.model.ChatType
import ru.andvl.chatkeep.infrastructure.repository.ChatSettingsRepository
import java.time.Instant

@Service
class ChatService(
    private val chatSettingsRepository: ChatSettingsRepository,
    private val metricsService: ru.andvl.chatkeep.metrics.BotMetricsService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun initMetrics() {
        updateActiveChatsMetric()
        logger.info("Initialized active chats metric on startup")
    }

    @Transactional
    fun registerChat(chatId: Long, chatTitle: String?, chatType: ChatType = ChatType.GROUP): ChatSettings {
        val existing = chatSettingsRepository.findByChatId(chatId)
        if (existing != null) {
            logger.debug("Chat $chatId already registered")
            return existing
        }

        val settings = ChatSettings(
            chatId = chatId,
            chatTitle = chatTitle,
            chatType = chatType,
            collectionEnabled = true
        )

        return chatSettingsRepository.save(settings).also {
            logger.info("Registered new $chatType: $chatId ($chatTitle)")
            updateActiveChatsMetric()
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

    fun getAllChats(): List<ChatSettings> =
        chatSettingsRepository.findAll()
            .filter { it.chatType != ChatType.CHANNEL }
            .toList()

    private fun updateActiveChatsMetric() {
        val activeCount = chatSettingsRepository.findAll()
            .count { it.collectionEnabled && it.chatType != ChatType.CHANNEL }
            .toLong()
        metricsService.setActiveChats(activeCount)
    }
}
