package com.chatkeep.admin.feature.dashboard.domain.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.dashboard.ActionResult

internal interface ActionsRepository {
    suspend fun restartBot(): AppResult<ActionResult>
}
