package com.chatkeep.admin.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LogsResponse(
    val lines: List<String>,
    val timestamp: String // ISO-8601 string
)
