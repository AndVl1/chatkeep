package ru.andvl.chatkeep.domain.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.ChatMessage
import ru.andvl.chatkeep.infrastructure.repository.MessageRepository
import java.time.Instant

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val chatService: ChatService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveMessage(
        telegramMessageId: Long,
        chatId: Long,
        userId: Long,
        username: String?,
        firstName: String?,
        lastName: String?,
        text: String,
        messageDate: Instant
    ): ChatMessage? {
        if (!chatService.isCollectionEnabled(chatId)) {
            logger.debug("Collection disabled for chat $chatId, skipping message")
            return null
        }

        if (messageRepository.existsByChatIdAndTelegramMessageId(chatId, telegramMessageId)) {
            logger.debug("Message $telegramMessageId already exists in chat $chatId")
            return null
        }

        val message = ChatMessage(
            telegramMessageId = telegramMessageId,
            chatId = chatId,
            userId = userId,
            username = username,
            firstName = firstName,
            lastName = lastName,
            text = text,
            messageDate = messageDate
        )

        return messageRepository.save(message).also {
            logger.debug("Saved message ${it.id} from user $userId in chat $chatId")
        }
    }

    fun getMessageCount(chatId: Long): Long = messageRepository.countByChatId(chatId)

    fun getUniqueUsersCount(chatId: Long): Long = messageRepository.countUniqueUsersByChatId(chatId)
}
