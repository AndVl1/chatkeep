package com.chatkeep.admin.core.domain.model

import kotlinx.datetime.Instant

data class LogsData(
    val lines: List<String>,
    val timestamp: Instant
)
