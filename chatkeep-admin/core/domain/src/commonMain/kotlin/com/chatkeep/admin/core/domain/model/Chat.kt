package com.chatkeep.admin.core.domain.model

data class Chat(
    val chatId: Long,
    val chatTitle: String,
    val messagesToday: Int,
    val messagesYesterday: Int
) {
    val trend: Trend
        get() = when {
            messagesToday > messagesYesterday -> Trend.UP
            messagesToday < messagesYesterday -> Trend.DOWN
            else -> Trend.SAME
        }
}
