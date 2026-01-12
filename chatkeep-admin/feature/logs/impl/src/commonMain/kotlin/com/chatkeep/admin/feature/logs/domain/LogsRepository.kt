package com.chatkeep.admin.feature.logs.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.logs.LogsData

internal interface LogsRepository {
    suspend fun getLogs(lines: Int = 100): AppResult<LogsData>
}
