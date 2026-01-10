package com.chatkeep.admin.core.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.Chat
import com.chatkeep.admin.core.domain.repository.ChatsRepository

class GetChatsUseCase(private val chatsRepository: ChatsRepository) {
    suspend operator fun invoke(): AppResult<List<Chat>> {
        return chatsRepository.getChats()
    }
}
