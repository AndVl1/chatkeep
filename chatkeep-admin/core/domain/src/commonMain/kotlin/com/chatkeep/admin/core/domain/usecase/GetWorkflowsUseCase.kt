package com.chatkeep.admin.core.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.Workflow
import com.chatkeep.admin.core.domain.repository.WorkflowsRepository

class GetWorkflowsUseCase(private val workflowsRepository: WorkflowsRepository) {
    suspend operator fun invoke(): AppResult<List<Workflow>> {
        return workflowsRepository.getWorkflows()
    }
}
