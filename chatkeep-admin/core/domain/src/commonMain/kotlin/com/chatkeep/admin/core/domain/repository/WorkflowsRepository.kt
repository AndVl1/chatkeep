package com.chatkeep.admin.core.domain.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.domain.model.Workflow

interface WorkflowsRepository {
    suspend fun getWorkflows(): AppResult<List<Workflow>>
    suspend fun triggerWorkflow(workflowId: String): AppResult<WorkflowTriggerResult>
}

data class WorkflowTriggerResult(
    val runId: Long,
    val url: String
)
