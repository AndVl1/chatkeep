package com.chatkeep.admin.feature.deploy.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.deploy.Workflow

internal class GetWorkflowsUseCase(private val workflowsRepository: WorkflowsRepository) {
    suspend operator fun invoke(): AppResult<List<Workflow>> {
        return workflowsRepository.getWorkflows()
    }
}
