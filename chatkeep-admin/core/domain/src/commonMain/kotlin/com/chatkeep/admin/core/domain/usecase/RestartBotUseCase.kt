package com.chatkeep.admin.core.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.repository.ActionResult
import com.chatkeep.admin.core.domain.repository.ActionsRepository

class RestartBotUseCase(private val actionsRepository: ActionsRepository) {
    suspend operator fun invoke(): AppResult<ActionResult> {
        return actionsRepository.restartBot()
    }
}
