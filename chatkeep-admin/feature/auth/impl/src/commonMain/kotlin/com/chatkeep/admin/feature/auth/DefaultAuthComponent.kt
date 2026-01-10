package com.chatkeep.admin.feature.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.BuildConfig
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.auth.data.repository.AuthRepositoryImpl
import com.chatkeep.admin.feature.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.launch

internal class DefaultAuthComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService,
    tokenStorage: TokenStorage,
    private val onSuccess: () -> Unit
) : AuthComponent, ComponentContext by componentContext {

    // Internal dependencies created within the component
    private val authRepository = AuthRepositoryImpl(apiService, tokenStorage)
    private val loginUseCase = LoginUseCase(authRepository)

    private val _state = MutableValue<AuthComponent.AuthState>(AuthComponent.AuthState.Idle)
    override val state: Value<AuthComponent.AuthState> = _state

    private val scope = componentScope()

    override fun onLoginClick() {
        performLogin()
    }

    override fun onRetry() {
        performLogin()
    }

    private fun performLogin() {
        scope.launch {
            _state.value = AuthComponent.AuthState.Loading

            // SECURITY: Mock authentication ONLY works in debug mode
            if (!BuildConfig.isDebug) {
                _state.value = AuthComponent.AuthState.Error(
                    "Telegram Login Widget integration required. " +
                    "Mock authentication is disabled in production builds."
                )
                return@launch
            }

            // DEBUG ONLY: Using mock data for development
            val mockTelegramData = TelegramLoginData(
                id = 123456789L,
                firstName = "Test",
                lastName = "User",
                username = "testuser",
                photoUrl = null,
                authDate = 1704067200L, // Mock timestamp: 2024-01-01
                hash = "mock_hash_for_development"
            )

            when (val result = loginUseCase(mockTelegramData)) {
                is AppResult.Success -> {
                    _state.value = AuthComponent.AuthState.Success
                    onSuccess()
                }
                is AppResult.Error -> {
                    _state.value = AuthComponent.AuthState.Error(result.message)
                }
                is AppResult.Loading -> {
                    _state.value = AuthComponent.AuthState.Loading
                }
            }
        }
    }
}
