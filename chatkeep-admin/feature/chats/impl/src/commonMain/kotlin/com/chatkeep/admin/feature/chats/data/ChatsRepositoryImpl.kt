package com.chatkeep.admin.feature.chats.data

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.chats.Chat
import com.chatkeep.admin.feature.chats.ChatType
import com.chatkeep.admin.feature.chats.domain.ChatsRepository

internal class ChatsRepositoryImpl(
    private val apiService: AdminApiService
) : ChatsRepository {

    override suspend fun getChats(): AppResult<List<Chat>> {
        return try {
            val response = apiService.getChats()
            val chats = response.map { chatResponse ->
                Chat(
                    chatId = chatResponse.chatId,
                    chatTitle = chatResponse.chatTitle,
                    chatType = chatResponse.chatType?.let { parseChatType(it) },
                    messagesToday = chatResponse.messagesToday,
                    messagesYesterday = chatResponse.messagesYesterday
                )
            }
            AppResult.Success(chats)
        } catch (e: Exception) {
            AppResult.Error("Failed to load chats: ${e.message}", e)
        }
    }

    private fun parseChatType(type: String): ChatType? {
        return when (type.uppercase()) {
            "PRIVATE" -> ChatType.PRIVATE
            "GROUP" -> ChatType.GROUP
            "SUPERGROUP" -> ChatType.SUPERGROUP
            "CHANNEL" -> ChatType.CHANNEL
            else -> null
        }
    }
}
