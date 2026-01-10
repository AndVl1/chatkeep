package ru.andvl.chatkeep.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
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
data class DashboardResponse(
    val serviceStatus: ServiceStatus,
    val deployInfo: DeployInfo,
    val quickStats: QuickStats
)

data class ServiceStatus(
    val running: Boolean,
    val uptime: Long? = null
)

data class DeployInfo(
    val commitSha: String?,
    val deployedAt: String?,
    val imageVersion: String?
)

data class QuickStats(
    val totalChats: Int,
    val messagesToday: Int,
    val messagesYesterday: Int
)

// Chat Statistics DTOs
data class ChatStatisticsResponse(
    val chatId: Long,
    val chatTitle: String?,
    val messagesToday: Int,
    val messagesYesterday: Int
)

// Workflow DTOs
data class WorkflowResponse(
    val id: String,
    val name: String,
    val filename: String,
    val lastRun: WorkflowRunResponse? = null
)

data class WorkflowRunResponse(
    val id: Long,
    val status: String,
    val conclusion: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val triggeredBy: String?,
    val url: String
)

data class TriggerWorkflowResponse(
    val success: Boolean,
    val message: String,
    val workflowId: String
)

// Logs DTOs
data class LogsResponse(
    val lines: List<String>,
    val timestamp: Instant
)

// Actions DTOs
data class ActionResponse(
    val success: Boolean,
    val message: String
)
