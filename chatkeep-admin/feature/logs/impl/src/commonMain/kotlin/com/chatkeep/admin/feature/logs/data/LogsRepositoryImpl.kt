package com.chatkeep.admin.feature.logs.data

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.logs.LogEntry
import com.chatkeep.admin.feature.logs.LogsData
import com.chatkeep.admin.feature.logs.domain.LogsRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

internal class LogsRepositoryImpl(
    private val apiService: AdminApiService
) : LogsRepository {

    override suspend fun getLogs(lines: Int): AppResult<LogsData> {
        return try {
            val response = apiService.getLogs(
                minutes = 60,  // Default: last hour
                level = "INFO",
                filter = null
            )

            // Parse timestamps safely, use defaults for empty strings
            val now = Clock.System.now()
            val defaultFromTime = now - 1.hours

            val logsData = LogsData(
                entries = response.entries.mapNotNull { entry ->
                    // Skip entries with empty timestamps
                    if (entry.timestamp.isBlank()) return@mapNotNull null
                    try {
                        LogEntry(
                            timestamp = Instant.parse(entry.timestamp),
                            level = entry.level,
                            logger = entry.logger,
                            message = entry.message
                        )
                    } catch (e: Exception) {
                        null // Skip malformed entries
                    }
                },
                totalCount = response.totalCount,
                fromTime = response.fromTime.takeIf { it.isNotBlank() }
                    ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    ?: defaultFromTime,
                toTime = response.toTime.takeIf { it.isNotBlank() }
                    ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    ?: now
            )
            AppResult.Success(logsData)
        } catch (e: Exception) {
            AppResult.Error("Failed to load logs: ${e.message}", e)
        }
    }
}
