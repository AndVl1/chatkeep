package com.chatkeep.admin.feature.dashboard.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.dashboard.DashboardInfo
import com.chatkeep.admin.feature.dashboard.domain.repository.DashboardRepository

internal class GetDashboardUseCase(private val dashboardRepository: DashboardRepository) {
    suspend operator fun invoke(): AppResult<DashboardInfo> {
        return dashboardRepository.getDashboard()
    }
}
