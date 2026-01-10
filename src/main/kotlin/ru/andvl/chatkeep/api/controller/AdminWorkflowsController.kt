package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.dto.TriggerWorkflowResponse
import ru.andvl.chatkeep.api.dto.WorkflowResponse
import ru.andvl.chatkeep.domain.service.github.GitHubService

@RestController
@RequestMapping("/api/v1/admin/workflows")
@Tag(name = "Admin - Workflows", description = "GitHub workflow management")
@SecurityRequirement(name = "BearerAuth")
class AdminWorkflowsController(
    private val gitHubService: GitHubService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "List GitHub workflows", description = "Returns list of available GitHub workflows with their last run status")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getWorkflows(): ResponseEntity<List<WorkflowResponse>> {
        val workflows = gitHubService.getWorkflows()
        logger.debug("Retrieved ${workflows.size} workflows from GitHub")
        return ResponseEntity.ok(workflows)
    }

    @PostMapping("/{workflowId}/trigger")
    @Operation(summary = "Trigger GitHub workflow", description = "Triggers a workflow_dispatch event for the specified workflow")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Workflow triggered successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "500", description = "Failed to trigger workflow")
    )
    fun triggerWorkflow(@PathVariable workflowId: String): ResponseEntity<TriggerWorkflowResponse> {
        logger.info("Triggering GitHub workflow: $workflowId")
        val response = gitHubService.triggerWorkflow(workflowId)
        return ResponseEntity.ok(response)
    }
}
