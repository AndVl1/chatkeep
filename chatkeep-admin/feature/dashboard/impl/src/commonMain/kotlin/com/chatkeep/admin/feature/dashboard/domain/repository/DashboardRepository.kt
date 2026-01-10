package com.chatkeep.admin.feature.dashboard.domain.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.dashboard.DashboardInfo

internal interface DashboardRepository {
    suspend fun getDashboard(): AppResult<DashboardInfo>
}
