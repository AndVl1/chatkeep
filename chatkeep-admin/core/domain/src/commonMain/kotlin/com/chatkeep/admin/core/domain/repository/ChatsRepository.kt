package com.chatkeep.admin.core.domain.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.Chat

interface ChatsRepository {
    suspend fun getChats(): AppResult<List<Chat>>
}
