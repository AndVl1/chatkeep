package com.chatkeep.admin.core.domain.usecase

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.repository.WorkflowTriggerResult
import com.chatkeep.admin.core.domain.repository.WorkflowsRepository

class TriggerWorkflowUseCase(private val workflowsRepository: WorkflowsRepository) {
    suspend operator fun invoke(workflowId: String): AppResult<WorkflowTriggerResult> {
        return workflowsRepository.triggerWorkflow(workflowId)
    }
}
