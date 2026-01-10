package com.chatkeep.admin.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val chatId: Long,
    val chatTitle: String,
    val messagesToday: Int,
    val messagesYesterday: Int
)
