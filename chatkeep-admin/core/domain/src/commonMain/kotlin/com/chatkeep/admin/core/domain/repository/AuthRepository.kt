package com.chatkeep.admin.core.domain.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.Admin
import com.chatkeep.admin.core.domain.model.AuthState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun login(telegramData: TelegramLoginData): AppResult<Admin>
    suspend fun logout()
    suspend fun restoreSession(): AppResult<Admin?>
}

data class TelegramLoginData(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val photoUrl: String?,
    val authDate: Long,
    val hash: String
)
