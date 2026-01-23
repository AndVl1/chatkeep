package ru.andvl.chatkeep.domain.model

import java.time.Instant

data class AntifloodSettings(
    val chatId: Long,
    val enabled: Boolean = false,
    val maxMessages: Int = 5,
    val timeWindowSeconds: Int = 5,
    val action: String = "MUTE",
    val actionDurationMinutes: Int? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null
)
