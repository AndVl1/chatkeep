package com.chatkeep.admin.core.domain.repository

import com.chatkeep.admin.core.common.AppResult

interface ActionsRepository {
    suspend fun restartBot(): AppResult<ActionResult>
}

data class ActionResult(
    val success: Boolean,
    val message: String
)
