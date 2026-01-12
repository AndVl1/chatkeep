package com.chatkeep.admin.feature.dashboard

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.network.AdminApiService

/**
 * Factory function to create a DashboardComponent.
 * This is the public API for creating dashboard components from outside the impl module.
 */
fun createDashboardComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService
): DashboardComponent {
    return DefaultDashboardComponent(
        componentContext = componentContext,
        apiService = apiService
    )
}
