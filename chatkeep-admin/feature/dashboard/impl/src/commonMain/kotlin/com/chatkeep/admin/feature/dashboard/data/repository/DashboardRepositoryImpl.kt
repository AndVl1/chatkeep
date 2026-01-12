package com.chatkeep.admin.feature.dashboard.data.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.dashboard.DashboardInfo
import com.chatkeep.admin.feature.dashboard.DeployInfo
import com.chatkeep.admin.feature.dashboard.QuickStats
import com.chatkeep.admin.feature.dashboard.ServiceStatus
import com.chatkeep.admin.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.datetime.Instant

internal class DashboardRepositoryImpl(
    private val apiService: AdminApiService
) : DashboardRepository {

    override suspend fun getDashboard(): AppResult<DashboardInfo> {
        return try {
            val response = apiService.getDashboard()
            val dashboard = DashboardInfo(
                serviceStatus = ServiceStatus(
                    running = response.serviceStatus.running,
                    uptime = response.serviceStatus.uptime
                ),
                deployInfo = DeployInfo(
                    commitSha = response.deployInfo.commitSha,
                    deployedAt = response.deployInfo.deployedAt?.let { Instant.parse(it) },
                    imageVersion = response.deployInfo.imageVersion
                ),
                quickStats = QuickStats(
                    totalChats = response.quickStats.totalChats,
                    messagesToday = response.quickStats.messagesToday,
                    messagesYesterday = response.quickStats.messagesYesterday
                )
            )
            AppResult.Success(dashboard)
        } catch (e: Exception) {
            AppResult.Error("Failed to load dashboard: ${e.message}", e)
        }
    }
}
