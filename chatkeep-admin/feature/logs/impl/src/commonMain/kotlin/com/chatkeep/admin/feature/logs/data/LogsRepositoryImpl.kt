package com.chatkeep.admin.feature.logs.data

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.logs.LogsData
import com.chatkeep.admin.feature.logs.domain.LogsRepository
import kotlinx.datetime.Instant

internal class LogsRepositoryImpl(
    private val apiService: AdminApiService
) : LogsRepository {

    override suspend fun getLogs(lines: Int): AppResult<LogsData> {
        return try {
            val response = apiService.getLogs(lines)
            val logsData = LogsData(
                lines = response.lines,
                timestamp = Instant.parse(response.timestamp)
            )
            AppResult.Success(logsData)
        } catch (e: Exception) {
            AppResult.Error("Failed to load logs: ${e.message}", e)
        }
    }
}
