package com.chatkeep.admin.feature.deploy.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.deploy.WorkflowTriggerResult

internal class TriggerWorkflowUseCase(private val workflowsRepository: WorkflowsRepository) {
    suspend operator fun invoke(workflowId: String): AppResult<WorkflowTriggerResult> {
        return workflowsRepository.triggerWorkflow(workflowId)
    }
}
