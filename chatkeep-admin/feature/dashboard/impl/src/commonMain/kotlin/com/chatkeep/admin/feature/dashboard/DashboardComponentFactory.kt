package com.chatkeep.admin.feature.dashboard

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.dashboard.data.repository.ActionsRepositoryImpl
import com.chatkeep.admin.feature.dashboard.data.repository.DashboardRepositoryImpl
import com.chatkeep.admin.feature.dashboard.domain.usecase.GetDashboardUseCase
import com.chatkeep.admin.feature.dashboard.domain.usecase.RestartBotUseCase

/**
 * Factory function to create a DashboardComponent.
 * This is the public API for creating dashboard components from outside the impl module.
 */
fun createDashboardComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService
): DashboardComponent {
    // Create dependencies
    val dashboardRepository = DashboardRepositoryImpl(apiService)
    val actionsRepository = ActionsRepositoryImpl(apiService)
    val getDashboardUseCase = GetDashboardUseCase(dashboardRepository)
    val restartBotUseCase = RestartBotUseCase(actionsRepository)

    return DefaultDashboardComponent(
        componentContext = componentContext,
        getDashboardUseCase = getDashboardUseCase,
        restartBotUseCase = restartBotUseCase
    )
}
