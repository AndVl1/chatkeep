package com.chatkeep.admin.feature.dashboard.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.dashboard.ActionResult
import com.chatkeep.admin.feature.dashboard.domain.repository.ActionsRepository

internal class RestartBotUseCase(private val actionsRepository: ActionsRepository) {
    suspend operator fun invoke(): AppResult<ActionResult> {
        return actionsRepository.restartBot()
    }
}
