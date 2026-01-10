package com.chatkeep.admin.core.data.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.data.mapper.toDomain
import com.chatkeep.admin.core.data.remote.AdminApiService
import com.chatkeep.admin.core.domain.model.DashboardInfo
import com.chatkeep.admin.core.domain.repository.DashboardRepository

class DashboardRepositoryImpl(
    private val apiService: AdminApiService
) : DashboardRepository {

    override suspend fun getDashboard(): AppResult<DashboardInfo> {
        return try {
            val response = apiService.getDashboard()
            AppResult.Success(response.toDomain())
        } catch (e: Exception) {
            AppResult.Error("Failed to load dashboard: ${e.message}", e)
        }
    }
}
