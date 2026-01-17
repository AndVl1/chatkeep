package com.chatkeep.admin.feature.logs

import kotlinx.datetime.Instant

data class LogEntry(
    val timestamp: Instant,
    val level: String,
    val logger: String,
    val message: String
)

data class LogsData(
    val entries: List<LogEntry>,
    val totalCount: Int,
    val fromTime: Instant,
    val toTime: Instant
)
