package com.chatkeep.admin.core.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.DashboardInfo
import com.chatkeep.admin.core.domain.repository.DashboardRepository

class GetDashboardUseCase(private val dashboardRepository: DashboardRepository) {
    suspend operator fun invoke(): AppResult<DashboardInfo> {
        return dashboardRepository.getDashboard()
    }
}
