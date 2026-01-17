package com.chatkeep.admin.feature.logs.data

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.logs.LogEntry
import com.chatkeep.admin.feature.logs.LogsData
import com.chatkeep.admin.feature.logs.domain.LogsRepository
import kotlinx.datetime.Instant

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
            val logsData = LogsData(
                entries = response.entries.map { entry ->
                    LogEntry(
                        timestamp = Instant.parse(entry.timestamp),
                        level = entry.level,
                        logger = entry.logger,
                        message = entry.message
                    )
                },
                totalCount = response.totalCount,
                fromTime = Instant.parse(response.fromTime),
                toTime = Instant.parse(response.toTime)
            )
            AppResult.Success(logsData)
        } catch (e: Exception) {
            AppResult.Error("Failed to load logs: ${e.message}", e)
        }
    }
}
