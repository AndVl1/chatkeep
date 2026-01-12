package com.chatkeep.admin.feature.auth

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.auth.domain.repository.AuthRepository

/**
 * Factory function to create an AuthComponent.
 * This is the public API for creating auth components from outside the impl module.
 */
fun createAuthComponent(
    componentContext: ComponentContext,
    authRepository: AuthRepository,
    apiService: AdminApiService,
    tokenStorage: TokenStorage,
    baseUrl: String,
    onSuccess: () -> Unit
): AuthComponent {
    return DefaultAuthComponent(
        componentContext = componentContext,
        authRepository = authRepository,
        apiService = apiService,
        tokenStorage = tokenStorage,
        baseUrl = baseUrl,
        onSuccess = onSuccess
    )
}
