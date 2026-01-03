package ru.andvl.chatkeep.domain.service.channelreply

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.core.type.TypeReference
import ru.andvl.chatkeep.domain.model.channelreply.ChannelReplySettings
import ru.andvl.chatkeep.domain.model.channelreply.MediaType
import ru.andvl.chatkeep.domain.model.channelreply.ReplyButton
import ru.andvl.chatkeep.infrastructure.repository.channelreply.ChannelReplySettingsRepository
import java.time.Instant

@Service
class ChannelReplyService(
    private val repository: ChannelReplySettingsRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getSettings(chatId: Long): ChannelReplySettings? {
        return repository.findByChatId(chatId)
    }

    @Transactional
    fun saveSettings(settings: ChannelReplySettings): ChannelReplySettings {
        return repository.save(settings)
    }

    @Transactional
    fun setEnabled(chatId: Long, enabled: Boolean) {
        val settings = repository.findByChatId(chatId) ?: ChannelReplySettings(chatId = chatId)
        val updated = settings.copy(
            enabled = enabled,
            updatedAt = Instant.now()
        )
        repository.save(updated)
        logger.info("Channel reply ${if (enabled) "enabled" else "disabled"} for chat $chatId")
    }

    @Transactional
    fun setText(chatId: Long, text: String) {
        val settings = repository.findByChatId(chatId) ?: ChannelReplySettings(chatId = chatId)
        val updated = settings.copy(
            replyText = text,
            updatedAt = Instant.now()
        )
        repository.save(updated)
        logger.info("Channel reply text set for chat $chatId")
    }

    @Transactional
    fun setMedia(chatId: Long, fileId: String, type: MediaType) {
        val settings = repository.findByChatId(chatId) ?: ChannelReplySettings(chatId = chatId)
        val updated = settings.copy(
            mediaFileId = fileId,
            mediaType = type.name,
            updatedAt = Instant.now()
        )
        repository.save(updated)
        logger.info("Channel reply media set for chat $chatId (type: ${type.name})")
    }

    @Transactional
    fun clearMedia(chatId: Long) {
        val settings = repository.findByChatId(chatId) ?: return
        val updated = settings.copy(
            mediaFileId = null,
            mediaType = null,
            updatedAt = Instant.now()
        )
        repository.save(updated)
        logger.info("Channel reply media cleared for chat $chatId")
    }

    @Transactional
    fun setButtons(chatId: Long, buttons: List<ReplyButton>) {
        val settings = repository.findByChatId(chatId) ?: ChannelReplySettings(chatId = chatId)
        val json = objectMapper.writeValueAsString(buttons)
        val updated = settings.copy(
            buttonsJson = json,
            updatedAt = Instant.now()
        )
        repository.save(updated)
        logger.info("Channel reply buttons set for chat $chatId (${buttons.size} buttons)")
    }

    @Transactional
    fun clearButtons(chatId: Long) {
        val settings = repository.findByChatId(chatId) ?: return
        val updated = settings.copy(
            buttonsJson = null,
            updatedAt = Instant.now()
        )
        repository.save(updated)
        logger.info("Channel reply buttons cleared for chat $chatId")
    }

    @Transactional
    fun deleteSettings(chatId: Long) {
        repository.deleteByChatId(chatId)
        logger.info("Channel reply settings deleted for chat $chatId")
    }

    fun parseButtons(buttonsJson: String?): List<ReplyButton> {
        if (buttonsJson.isNullOrBlank()) return emptyList()
        return try {
            objectMapper.readValue(buttonsJson, object : TypeReference<List<ReplyButton>>() {})
        } catch (e: Exception) {
            logger.error("Failed to parse buttons JSON: ${e.message}", e)
            emptyList()
        }
    }
}
