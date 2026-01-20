package ru.andvl.chatkeep.domain.model

import java.time.Instant

data class WelcomeSettings(
    val chatId: Long,
    val enabled: Boolean = true,
    val messageText: String? = null,
    val sendToChat: Boolean = true,
    val deleteAfterSeconds: Int? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null
)
