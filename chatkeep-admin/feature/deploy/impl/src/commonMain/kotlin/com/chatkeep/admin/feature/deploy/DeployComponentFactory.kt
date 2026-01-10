package com.chatkeep.admin.feature.deploy

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.network.AdminApiService

/**
 * Factory function to create a DeployComponent.
 * This is the public API for creating deploy components from outside the impl module.
 */
fun createDeployComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService
): DeployComponent {
    return DefaultDeployComponent(
        componentContext = componentContext,
        apiService = apiService
    )
}
