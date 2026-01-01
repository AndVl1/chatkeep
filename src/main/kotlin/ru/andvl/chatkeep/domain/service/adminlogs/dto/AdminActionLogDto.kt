package ru.andvl.chatkeep.domain.service.adminlogs.dto

import java.time.Instant

data class AdminActionLogDto(
    val actionType: String,
    val userId: Long,
    val issuedById: Long,
    val durationSeconds: Long?,
    val reason: String?,
    val messageText: String?,
    val source: String,
    val timestamp: Instant
)
