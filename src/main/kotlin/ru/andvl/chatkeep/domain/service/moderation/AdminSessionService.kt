package ru.andvl.chatkeep.domain.service.moderation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.domain.model.moderation.AdminSession
import ru.andvl.chatkeep.infrastructure.repository.moderation.AdminSessionRepository

@Service
class AdminSessionService(
    private val adminSessionRepository: AdminSessionRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun connect(userId: Long, chatId: Long, chatTitle: String?): AdminSession {
        // Delete existing session if any
        adminSessionRepository.deleteByUserId(userId)

        val session = AdminSession(
            userId = userId,
            connectedChatId = chatId,
            connectedChatTitle = chatTitle
        )

        val saved = adminSessionRepository.save(session)
        logger.info("Created admin session: userId=$userId, chatId=$chatId")
        return saved
    }

    fun disconnect(userId: Long) {
        adminSessionRepository.deleteByUserId(userId)
        logger.info("Disconnected admin session: userId=$userId")
    }

    fun getSession(userId: Long): AdminSession? {
        return adminSessionRepository.findByUserId(userId)
    }

    fun formatReplyPrefix(session: AdminSession): String {
        val chatName = session.connectedChatTitle ?: "Chat ${session.connectedChatId}"
        return "**[$chatName]**"
    }
}
