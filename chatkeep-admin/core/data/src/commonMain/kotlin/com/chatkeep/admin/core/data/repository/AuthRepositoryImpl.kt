package com.chatkeep.admin.core.data.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.data.local.TokenStorage
import com.chatkeep.admin.core.data.mapper.toDomain
import com.chatkeep.admin.core.data.mapper.toRequest
import com.chatkeep.admin.core.data.remote.AdminApiService
import com.chatkeep.admin.core.domain.model.Admin
import com.chatkeep.admin.core.domain.model.AuthState
import com.chatkeep.admin.core.domain.repository.AuthRepository
import com.chatkeep.admin.core.domain.repository.TelegramLoginData
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
            val response = apiService.login(telegramData.toRequest())
            tokenStorage.saveToken(response.token)
            val admin = response.user.toDomain()
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

            val admin = apiService.getMe().toDomain()
            _authState.value = AuthState.Authenticated(admin, token)
            AppResult.Success(admin)
        } catch (e: Exception) {
            tokenStorage.clearToken()
            _authState.value = AuthState.NotAuthenticated
            AppResult.Error("Session restore failed: ${e.message}", e)
        }
    }
}
