package ru.andvl.chatkeep.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.Instant

// Auth DTOs
data class AdminLoginRequest(
    @field:NotNull
    @field:Positive
    val id: Long,

    @field:NotBlank
    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String? = null,

    val username: String? = null,

    @JsonProperty("photo_url")
    val photoUrl: String? = null,

    @field:NotNull
    @field:Positive
    @JsonProperty("auth_date")
    val authDate: Long,

    @field:NotBlank
    val hash: String
)

// Dashboard DTOs
@Schema(description = "Dashboard overview with service status and statistics")
data class DashboardResponse(
    @Schema(description = "Service status information")
    val serviceStatus: ServiceStatus,

    @Schema(description = "Deployment information")
    val deployInfo: DeployInfo,

    @Schema(description = "Quick statistics")
    val quickStats: QuickStats
)

@Schema(description = "Service health status")
data class ServiceStatus(
    @Schema(description = "Whether the service is running", example = "true")
    val running: Boolean,

    @Schema(description = "Uptime in seconds", example = "86400", nullable = true)
    val uptime: Long? = null
)

@Schema(description = "Deployment metadata")
data class DeployInfo(
    @Schema(description = "Git commit SHA", example = "a1b2c3d4", nullable = true)
    val commitSha: String?,

    @Schema(description = "Deployment timestamp", example = "2026-01-17T12:00:00Z", nullable = true)
    val deployedAt: String?,

    @Schema(description = "Docker image version", example = "v1.2.3", nullable = true)
    val imageVersion: String?
)

@Schema(description = "Quick statistics summary")
data class QuickStats(
    @Schema(description = "Total number of registered chats", example = "42")
    val totalChats: Int,

    @Schema(description = "Messages received today", example = "150")
    val messagesToday: Int,

    @Schema(description = "Messages received yesterday", example = "120")
    val messagesYesterday: Int
)

// Chat Statistics DTOs
data class ChatStatisticsResponse(
    val chatId: Long,
    val chatTitle: String?,
    val totalMessages: Long,
    val uniqueUsers: Long,
    val collectionEnabled: Boolean,
    // Optional time-based statistics (not all endpoints populate these)
    val messagesToday: Int? = null,
    val messagesYesterday: Int? = null
)

// Workflow DTOs
@Schema(description = "GitHub workflow information")
data class WorkflowResponse(
    @Schema(description = "Workflow ID", example = "deploy.yml")
    val id: String,

    @Schema(description = "Workflow name", example = "Deploy to Production")
    val name: String,

    @Schema(description = "Workflow filename", example = "deploy.yml")
    val filename: String,

    @Schema(description = "Last workflow run details", nullable = true)
    val lastRun: WorkflowRunResponse? = null
)

@Schema(description = "Workflow run details")
data class WorkflowRunResponse(
    @Schema(description = "Run ID", example = "123456789")
    val id: Long,

    @Schema(description = "Run status", example = "completed")
    val status: String,

    @Schema(description = "Run conclusion", example = "success", nullable = true)
    val conclusion: String?,

    @Schema(description = "Run creation timestamp", example = "2026-01-17T12:00:00Z")
    val createdAt: Instant,

    @Schema(description = "Run last update timestamp", example = "2026-01-17T12:05:00Z")
    val updatedAt: Instant,

    @Schema(description = "User who triggered the run", example = "github-actions", nullable = true)
    val triggeredBy: String?,

    @Schema(description = "GitHub Actions run URL", example = "https://github.com/owner/repo/actions/runs/123456789")
    val url: String
)

@Schema(description = "Workflow trigger response")
data class TriggerWorkflowResponse(
    @Schema(description = "Whether the trigger was successful", example = "true")
    val success: Boolean,

    @Schema(description = "Response message", example = "Workflow triggered successfully")
    val message: String,

    @Schema(description = "Workflow ID", example = "deploy.yml")
    val workflowId: String
)

// Logs DTOs
@Schema(description = "Log entry")
data class LogEntry(
    @Schema(description = "Log timestamp", example = "2026-01-17T12:00:00Z")
    val timestamp: Instant,

    @Schema(description = "Log level", example = "INFO")
    val level: String,

    @Schema(description = "Logger name", example = "ru.andvl.chatkeep.bot.ChatkeepBot")
    val logger: String,

    @Schema(description = "Log message", example = "Bot started successfully")
    val message: String
)

@Schema(description = "Paginated logs response")
data class LogsResponse(
    @Schema(description = "List of log entries")
    val entries: List<LogEntry>,

    @Schema(description = "Total number of log entries", example = "500")
    val totalCount: Int,

    @Schema(description = "Start of time range", example = "2026-01-17T00:00:00Z")
    val fromTime: Instant,

    @Schema(description = "End of time range", example = "2026-01-17T23:59:59Z")
    val toTime: Instant
)

// Actions DTOs
@Schema(description = "Generic action response")
data class ActionResponse(
    @Schema(description = "Whether the action was successful", example = "true")
    val success: Boolean,

    @Schema(description = "Action result message", example = "Action completed successfully")
    val message: String
)
