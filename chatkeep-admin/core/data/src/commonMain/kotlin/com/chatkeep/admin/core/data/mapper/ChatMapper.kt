package com.chatkeep.admin.core.data.mapper

import com.chatkeep.admin.core.data.remote.dto.ChatResponse
import com.chatkeep.admin.core.domain.model.Chat

fun ChatResponse.toDomain(): Chat {
    return Chat(
        chatId = chatId,
        chatTitle = chatTitle,
        messagesToday = messagesToday,
        messagesYesterday = messagesYesterday
    )
}
