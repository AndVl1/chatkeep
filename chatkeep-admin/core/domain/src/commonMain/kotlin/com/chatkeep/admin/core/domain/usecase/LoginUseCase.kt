package com.chatkeep.admin.core.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.Admin
import com.chatkeep.admin.core.domain.repository.AuthRepository
import com.chatkeep.admin.core.domain.repository.TelegramLoginData

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(data: TelegramLoginData): AppResult<Admin> {
        return authRepository.login(data)
    }
}
