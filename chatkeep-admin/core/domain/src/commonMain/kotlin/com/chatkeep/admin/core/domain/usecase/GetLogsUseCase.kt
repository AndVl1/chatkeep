package com.chatkeep.admin.core.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.LogsData
import com.chatkeep.admin.core.domain.repository.LogsRepository

class GetLogsUseCase(private val logsRepository: LogsRepository) {
    suspend operator fun invoke(lines: Int = 100): AppResult<LogsData> {
        return logsRepository.getLogs(lines)
    }
}
