package com.chatkeep.admin.feature.deploy.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.deploy.Workflow
import com.chatkeep.admin.feature.deploy.WorkflowTriggerResult

internal interface WorkflowsRepository {
    suspend fun getWorkflows(): AppResult<List<Workflow>>
    suspend fun triggerWorkflow(workflowId: String): AppResult<WorkflowTriggerResult>
}
