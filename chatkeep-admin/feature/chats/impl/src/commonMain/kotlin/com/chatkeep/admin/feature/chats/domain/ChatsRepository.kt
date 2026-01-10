package com.chatkeep.admin.feature.chats.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.chats.Chat

internal interface ChatsRepository {
    suspend fun getChats(): AppResult<List<Chat>>
}
