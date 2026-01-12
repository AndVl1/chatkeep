package ru.andvl.chatkeep.domain.service.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import ru.andvl.chatkeep.api.dto.TriggerWorkflowResponse
import ru.andvl.chatkeep.api.dto.WorkflowResponse
import ru.andvl.chatkeep.api.dto.WorkflowRunResponse
import java.time.Instant

@Service
class GitHubService(
    @Value("\${github.token:}") private val githubToken: String,
    @Value("\${github.owner:AndVl1}") private val owner: String,
    @Value("\${github.repo:chatkeep}") private val repo: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient = RestClient.builder()
        .baseUrl("https://api.github.com")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $githubToken")
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
        .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
        .build()

    fun getWorkflows(): List<WorkflowResponse> {
        if (githubToken.isBlank()) {
            logger.warn("GitHub token not configured, returning empty workflows list")
            return emptyList()
        }

        return try {
            val response = restClient.get()
                .uri("/repos/$owner/$repo/actions/workflows")
                .retrieve()
                .body<GitHubWorkflowsResponse>()

            response?.workflows?.map { workflow ->
                val lastRun = getLastWorkflowRun(workflow.id.toString())

                WorkflowResponse(
                    id = workflow.id.toString(),
                    name = workflow.name,
                    filename = workflow.path.substringAfterLast("/"),
                    lastRun = lastRun
                )
            } ?: emptyList()
        } catch (e: Exception) {
            logger.error("Error fetching workflows from GitHub: ${e.message}", e)
            emptyList()
        }
    }

    fun triggerWorkflow(workflowId: String): TriggerWorkflowResponse {
        if (githubToken.isBlank()) {
            logger.error("GitHub token not configured")
            return TriggerWorkflowResponse(
                success = false,
                message = "GitHub token not configured",
                workflowId = workflowId
            )
        }

        return try {
            // Trigger workflow dispatch event
            restClient.post()
                .uri("/repos/$owner/$repo/actions/workflows/$workflowId/dispatches")
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("ref" to "main"))
                .retrieve()
                .toBodilessEntity()

            logger.info("Triggered GitHub workflow: $workflowId")

            TriggerWorkflowResponse(
                success = true,
                message = "Workflow triggered successfully",
                workflowId = workflowId
            )
        } catch (e: Exception) {
            logger.error("Error triggering workflow $workflowId: ${e.message}", e)
            TriggerWorkflowResponse(
                success = false,
                message = "Failed to trigger workflow: ${e.message}",
                workflowId = workflowId
            )
        }
    }

    private fun getLastWorkflowRun(workflowId: String): WorkflowRunResponse? {
        return try {
            val response = restClient.get()
                .uri("/repos/$owner/$repo/actions/workflows/$workflowId/runs?per_page=1")
                .retrieve()
                .body<GitHubWorkflowRunsResponse>()

            response?.workflowRuns?.firstOrNull()?.let { run ->
                WorkflowRunResponse(
                    id = run.id,
                    status = run.status,
                    conclusion = run.conclusion,
                    createdAt = Instant.parse(run.createdAt),
                    updatedAt = Instant.parse(run.updatedAt),
                    triggeredBy = run.actor?.login,
                    url = run.htmlUrl
                )
            }
        } catch (e: Exception) {
            logger.debug("Error fetching last run for workflow $workflowId: ${e.message}")
            null
        }
    }

    // GitHub API response models
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class GitHubWorkflowsResponse(
        val workflows: List<GitHubWorkflow>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class GitHubWorkflow(
        val id: Long,
        val name: String,
        val path: String,
        val state: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class GitHubWorkflowRunsResponse(
        @JsonProperty("workflow_runs")
        val workflowRuns: List<GitHubWorkflowRun>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class GitHubWorkflowRun(
        val id: Long,
        val status: String,
        val conclusion: String?,
        @JsonProperty("created_at")
        val createdAt: String,
        @JsonProperty("updated_at")
        val updatedAt: String,
        @JsonProperty("html_url")
        val htmlUrl: String,
        val actor: GitHubUser?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class GitHubUser(
        val login: String
    )
}
