package com.chatkeep.admin.core.data.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.data.mapper.toDomain
import com.chatkeep.admin.core.data.remote.AdminApiService
import com.chatkeep.admin.core.domain.model.Workflow
import com.chatkeep.admin.core.domain.repository.WorkflowTriggerResult
import com.chatkeep.admin.core.domain.repository.WorkflowsRepository

class WorkflowsRepositoryImpl(
    private val apiService: AdminApiService
) : WorkflowsRepository {

    override suspend fun getWorkflows(): AppResult<List<Workflow>> {
        return try {
            val response = apiService.getWorkflows()
            AppResult.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            AppResult.Error("Failed to load workflows: ${e.message}", e)
        }
    }

    override suspend fun triggerWorkflow(workflowId: String): AppResult<WorkflowTriggerResult> {
        return try {
            val response = apiService.triggerWorkflow(workflowId)
            AppResult.Success(
                WorkflowTriggerResult(
                    runId = response.runId,
                    url = response.url
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to trigger workflow: ${e.message}", e)
        }
    }
}
