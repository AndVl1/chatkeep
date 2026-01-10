package com.chatkeep.admin.core.domain.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.DashboardInfo

interface DashboardRepository {
    suspend fun getDashboard(): AppResult<DashboardInfo>
}
