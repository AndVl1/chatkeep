package ru.andvl.chatkeep.domain.service

import org.springframework.stereotype.Service
import ru.andvl.chatkeep.domain.model.ChatStatistics

@Service
class AdminService(
    private val chatService: ChatService,
    private val messageService: MessageService
) {

    fun getStatistics(chatId: Long): ChatStatistics? {
        val settings = chatService.getSettings(chatId) ?: return null

        return ChatStatistics(
            chatId = chatId,
            chatTitle = settings.chatTitle,
            totalMessages = messageService.getMessageCount(chatId),
            uniqueUsers = messageService.getUniqueUsersCount(chatId),
            collectionEnabled = settings.collectionEnabled
        )
    }

    fun getAdminChats(adminChatIds: List<Long>): List<ChatStatistics> {
        return adminChatIds.mapNotNull { getStatistics(it) }
    }
}
