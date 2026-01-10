package com.chatkeep.admin.core.data.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.data.mapper.toDomain
import com.chatkeep.admin.core.data.remote.AdminApiService
import com.chatkeep.admin.core.domain.model.LogsData
import com.chatkeep.admin.core.domain.repository.LogsRepository

class LogsRepositoryImpl(
    private val apiService: AdminApiService
) : LogsRepository {

    override suspend fun getLogs(lines: Int): AppResult<LogsData> {
        return try {
            val response = apiService.getLogs(lines)
            AppResult.Success(response.toDomain())
        } catch (e: Exception) {
            AppResult.Error("Failed to load logs: ${e.message}", e)
        }
    }
}
