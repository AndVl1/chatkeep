package com.chatkeep.admin.core.domain.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.LogsData

interface LogsRepository {
    suspend fun getLogs(lines: Int = 100): AppResult<LogsData>
}
