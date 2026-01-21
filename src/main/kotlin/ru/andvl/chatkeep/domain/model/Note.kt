package ru.andvl.chatkeep.domain.model

import java.time.Instant

data class Note(
    val id: Long = 0,
    val chatId: Long,
    val noteName: String,
    val content: String,
    val createdBy: Long,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null
)
