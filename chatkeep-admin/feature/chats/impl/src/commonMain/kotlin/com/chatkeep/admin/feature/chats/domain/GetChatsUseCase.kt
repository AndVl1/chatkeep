package com.chatkeep.admin.feature.chats.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.chats.Chat

internal class GetChatsUseCase(private val chatsRepository: ChatsRepository) {
    suspend operator fun invoke(): AppResult<List<Chat>> {
        return chatsRepository.getChats()
    }
}
