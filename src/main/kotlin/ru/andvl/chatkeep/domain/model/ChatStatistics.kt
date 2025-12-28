package ru.andvl.chatkeep.domain.model

data class ChatStatistics(
    val chatId: Long,
    val chatTitle: String?,
    val totalMessages: Long,
    val uniqueUsers: Long,
    val collectionEnabled: Boolean
)
