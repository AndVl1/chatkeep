package com.chatkeep.admin.feature.auth.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.auth.Admin
import com.chatkeep.admin.feature.auth.TelegramLoginData
import com.chatkeep.admin.feature.auth.domain.repository.AuthRepository

internal class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(data: TelegramLoginData): AppResult<Admin> {
        return authRepository.login(data)
    }
}
