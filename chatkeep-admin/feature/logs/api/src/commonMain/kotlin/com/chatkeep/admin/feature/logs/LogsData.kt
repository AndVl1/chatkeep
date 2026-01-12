package com.chatkeep.admin.feature.logs

import kotlinx.datetime.Instant

data class LogsData(
    val lines: List<String>,
    val timestamp: Instant
)
