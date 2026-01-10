package com.chatkeep.admin.feature.auth

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.network.AdminApiService

/**
 * Factory function to create an AuthComponent.
 * This is the public API for creating auth components from outside the impl module.
 */
fun createAuthComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService,
    tokenStorage: TokenStorage,
    onSuccess: () -> Unit
): AuthComponent {
    return DefaultAuthComponent(
        componentContext = componentContext,
        apiService = apiService,
        tokenStorage = tokenStorage,
        onSuccess = onSuccess
    )
}
