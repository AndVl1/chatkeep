package com.chatkeep.admin.feature.deploy.data

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.deploy.Workflow
import com.chatkeep.admin.feature.deploy.WorkflowRun
import com.chatkeep.admin.feature.deploy.WorkflowStatus
import com.chatkeep.admin.feature.deploy.WorkflowTriggerResult
import com.chatkeep.admin.feature.deploy.domain.WorkflowsRepository
import kotlinx.datetime.Instant

internal class WorkflowsRepositoryImpl(
    private val apiService: AdminApiService
) : WorkflowsRepository {

    override suspend fun getWorkflows(): AppResult<List<Workflow>> {
        return try {
            val response = apiService.getWorkflows()
            val workflows = response.map { workflowResponse ->
                Workflow(
                    id = workflowResponse.id,
                    name = workflowResponse.name,
                    filename = workflowResponse.filename,
                    lastRun = workflowResponse.lastRun?.let { runDto ->
                        WorkflowRun(
                            id = runDto.id,
                            status = runDto.status.toWorkflowStatus(),
                            conclusion = runDto.conclusion,
                            createdAt = Instant.parse(runDto.createdAt),
                            updatedAt = Instant.parse(runDto.updatedAt),
                            triggeredBy = runDto.triggeredBy,
                            url = runDto.url
                        )
                    }
                )
            }
            AppResult.Success(workflows)
        } catch (e: Exception) {
            AppResult.Error("Failed to load workflows: ${e.message}", e)
        }
    }

    override suspend fun triggerWorkflow(workflowId: String): AppResult<WorkflowTriggerResult> {
        return try {
            val response = apiService.triggerWorkflow(workflowId)
            val result = WorkflowTriggerResult(
                runId = response.runId,
                url = response.url
            )
            AppResult.Success(result)
        } catch (e: Exception) {
            AppResult.Error("Failed to trigger workflow: ${e.message}", e)
        }
    }

    private fun String.toWorkflowStatus(): WorkflowStatus {
        return when (this.lowercase()) {
            "queued" -> WorkflowStatus.QUEUED
            "in_progress", "in progress" -> WorkflowStatus.IN_PROGRESS
            "completed" -> WorkflowStatus.COMPLETED
            else -> WorkflowStatus.UNKNOWN
        }
    }
}
