package com.chatkeep.admin.feature.auth.data.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.core.network.LoginRequest
import com.chatkeep.admin.feature.auth.Admin
import com.chatkeep.admin.feature.auth.AuthState
import com.chatkeep.admin.feature.auth.TelegramLoginData
import com.chatkeep.admin.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val apiService: AdminApiService,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun login(telegramData: TelegramLoginData): AppResult<Admin> {
        return try {
            val request = LoginRequest(
                id = telegramData.id,
                firstName = telegramData.firstName,
                lastName = telegramData.lastName,
                username = telegramData.username,
                photoUrl = telegramData.photoUrl,
                authDate = telegramData.authDate,
                hash = telegramData.hash
            )
            val response = apiService.login(request)
            tokenStorage.saveToken(response.token)
            val admin = Admin(
                id = response.user.id,
                firstName = response.user.firstName,
                lastName = response.user.lastName,
                username = response.user.username,
                photoUrl = response.user.photoUrl
            )
            _authState.value = AuthState.Authenticated(admin, response.token)
            AppResult.Success(admin)
        } catch (e: Exception) {
            _authState.value = AuthState.NotAuthenticated
            AppResult.Error("Login failed: ${e.message}", e)
        }
    }

    override suspend fun logout() {
        tokenStorage.clearToken()
        _authState.value = AuthState.NotAuthenticated
    }

    override suspend fun restoreSession(): AppResult<Admin?> {
        return try {
            val token = tokenStorage.getToken()
            if (token == null) {
                _authState.value = AuthState.NotAuthenticated
                return AppResult.Success(null)
            }

            val response = apiService.getMe()
            val admin = Admin(
                id = response.id,
                firstName = response.firstName,
                lastName = response.lastName,
                username = response.username,
                photoUrl = response.photoUrl
            )
            _authState.value = AuthState.Authenticated(admin, token)
            AppResult.Success(admin)
        } catch (e: Exception) {
            tokenStorage.clearToken()
            _authState.value = AuthState.NotAuthenticated
            AppResult.Error("Session restore failed: ${e.message}", e)
        }
    }
}
