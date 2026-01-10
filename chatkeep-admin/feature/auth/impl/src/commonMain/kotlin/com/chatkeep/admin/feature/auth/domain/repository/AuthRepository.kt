package com.chatkeep.admin.feature.auth.domain.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.auth.Admin
import com.chatkeep.admin.feature.auth.AuthState
import com.chatkeep.admin.feature.auth.TelegramLoginData
import kotlinx.coroutines.flow.StateFlow

internal interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun login(telegramData: TelegramLoginData): AppResult<Admin>
    suspend fun logout()
    suspend fun restoreSession(): AppResult<Admin?>
}
