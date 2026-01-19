package com.chatkeep.admin.feature.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.BuildConfig
import com.chatkeep.admin.core.common.DeepLinkData
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.common.openInBrowser
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.auth.domain.repository.AuthRepository
import com.chatkeep.admin.feature.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.launch
import kotlin.random.Random

internal class DefaultAuthComponent(
    componentContext: ComponentContext,
    authRepository: AuthRepository,
    apiService: AdminApiService,
    tokenStorage: TokenStorage,
    private val baseUrl: String,
    private val onSuccess: () -> Unit
) : AuthComponent, ComponentContext by componentContext {

    init {
        // Validate baseUrl for security
        require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://localhost")) {
            "Base URL must use HTTPS (or localhost for development)"
        }
    }

    // Use injected repository (shared with RootComponent for navigation)
    private val loginUseCase = LoginUseCase(authRepository)

    private val _state = MutableValue<AuthComponent.AuthState>(AuthComponent.AuthState.Idle)
    override val state: Value<AuthComponent.AuthState> = _state

    private val scope = componentScope()

    // Store state token for CSRF validation
    // Note: In-memory storage means token is lost on process death during OAuth
    // This is acceptable as user will need to retry login in that rare case
    private var expectedStateToken: String? = null

    override fun onLoginClick() {
        performOAuthLogin()
    }

    override fun onRetry() {
        performOAuthLogin()
    }

    override fun onDeepLinkReceived(data: DeepLinkData) {
        scope.launch {
            // Validate state token
            if (data.state != expectedStateToken) {
                _state.value = AuthComponent.AuthState.Error("Invalid state token. Possible CSRF attack.")
                expectedStateToken = null
                return@launch
            }

            // Clear expected token
            expectedStateToken = null

            // Set loading state
            _state.value = AuthComponent.AuthState.Loading

            // Convert DeepLinkData to TelegramLoginData
            val telegramData = TelegramLoginData(
                id = data.id,
                firstName = data.firstName,
                lastName = data.lastName,
                username = data.username,
                photoUrl = data.photoUrl,
                authDate = data.authDate,
                hash = data.hash
            )

            // Perform login
            when (val result = loginUseCase(telegramData)) {
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

    private fun performOAuthLogin() {
        // Generate random state token for CSRF protection
        val stateToken = generateStateToken()
        expectedStateToken = stateToken

        // Build OAuth URL using environment-aware backend URL
        val oauthUrl = "${BuildConfig.authBackendUrl}/auth/telegram-login?state=$stateToken"

        // Open browser
        try {
            openInBrowser(oauthUrl)
            _state.value = AuthComponent.AuthState.Idle  // Keep idle while waiting for callback
        } catch (e: Exception) {
            _state.value = AuthComponent.AuthState.Error("Failed to open browser: ${e.message}")
            expectedStateToken = null
        }
    }

    private fun generateStateToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}
