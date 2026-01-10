package com.chatkeep.admin.feature.logs.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.logs.LogsData

internal class GetLogsUseCase(private val logsRepository: LogsRepository) {
    suspend operator fun invoke(lines: Int = 100): AppResult<LogsData> {
        return logsRepository.getLogs(lines)
    }
}
