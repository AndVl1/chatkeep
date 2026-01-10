package com.chatkeep.admin.feature.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.BuildConfig
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.domain.usecase.LoginUseCase
import kotlinx.coroutines.launch

class DefaultAuthComponent(
    componentContext: ComponentContext,
    private val loginUseCase: LoginUseCase,
    private val onSuccess: () -> Unit
) : AuthComponent, ComponentContext by componentContext {

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
            val mockTelegramData = com.chatkeep.admin.core.domain.repository.TelegramLoginData(
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
