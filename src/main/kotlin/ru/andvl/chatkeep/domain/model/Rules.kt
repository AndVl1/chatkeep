package ru.andvl.chatkeep.domain.model

import java.time.Instant

data class Rules(
    val chatId: Long,
    val rulesText: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null
)
