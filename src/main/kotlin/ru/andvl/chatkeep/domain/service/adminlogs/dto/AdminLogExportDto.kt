package ru.andvl.chatkeep.domain.service.adminlogs.dto

data class AdminLogExportDto(
    val chatId: Long,
    val exportedAt: String,
    val totalActions: Int,
    val actions: List<AdminActionLogDto>
)
