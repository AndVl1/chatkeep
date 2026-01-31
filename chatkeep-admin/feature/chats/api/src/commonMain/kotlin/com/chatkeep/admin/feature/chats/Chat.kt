package com.chatkeep.admin.feature.chats

import com.chatkeep.admin.feature.dashboard.Trend

data class Chat(
    val chatId: Long,
    val chatTitle: String?,
    val chatType: ChatType?,
    val messagesToday: Int,
    val messagesYesterday: Int
) {
    val trend: Trend
        get() = when {
            messagesToday > messagesYesterday -> Trend.UP
            messagesToday < messagesYesterday -> Trend.DOWN
            else -> Trend.SAME
        }

    val displayTitle: String
        get() = chatTitle ?: run {
            val typePrefix = when (chatType) {
                ChatType.CHANNEL -> "📢 Channel"
                ChatType.SUPERGROUP -> "💬 Supergroup"
                ChatType.GROUP -> "👥 Group"
                ChatType.PRIVATE -> "👤 Private"
                null -> "Chat"
            }
            "$typePrefix #$chatId"
        }

    val typeIcon: String
        get() = when (chatType) {
            ChatType.CHANNEL -> "📢"
            ChatType.SUPERGROUP -> "💬"
            ChatType.GROUP -> "👥"
            ChatType.PRIVATE -> "👤"
            null -> "💬"
        }
}

enum class ChatType {
    PRIVATE,
    GROUP,
    SUPERGROUP,
    CHANNEL
}
