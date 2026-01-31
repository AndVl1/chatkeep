package com.chatkeep.admin.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Auth DTOs
@Serializable
data class LoginRequest(
    val id: Long,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String? = null,
    val username: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null,
    @SerialName("auth_date")
    val authDate: Long,
    val hash: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val expiresIn: Long? = null,
    val user: AdminResponse
)

@Serializable
data class AdminResponse(
    val id: Long,
    val firstName: String,
    val lastName: String? = null,
    val username: String? = null,
    val photoUrl: String? = null
)

// Dashboard DTOs
@Serializable
data class DashboardResponse(
    val serviceStatus: ServiceStatusDto,
    val deployInfo: DeployInfoDto,
    val quickStats: QuickStatsDto
)

@Serializable
data class ServiceStatusDto(
    val running: Boolean,
    val uptime: Long
)

@Serializable
data class DeployInfoDto(
    val commitSha: String?,
    val deployedAt: String?,
    val imageVersion: String?
)

@Serializable
data class QuickStatsDto(
    val totalChats: Int,
    val messagesToday: Int,
    val messagesYesterday: Int
)

@Serializable
data class ActionResponse(
    val success: Boolean,
    val message: String
)

// Chats DTOs
@Serializable
data class ChatResponse(
    val chatId: Long,
    val chatTitle: String? = null,
    val chatType: String? = null,
    val messagesToday: Int,
    val messagesYesterday: Int
)

// Logs DTOs
@Serializable
data class LogEntry(
    val timestamp: String = "",  // Instant serialized as ISO-8601 string
    val level: String = "INFO",
    val logger: String = "",
    val message: String = ""
)

@Serializable
data class LogsResponse(
    val entries: List<LogEntry> = emptyList(),
    val totalCount: Int = 0,
    val fromTime: String = "",  // Instant serialized as ISO-8601 string
    val toTime: String = ""     // Instant serialized as ISO-8601 string
)

// Workflow DTOs
@Serializable
data class WorkflowResponse(
    val id: String,
    val name: String,
    val filename: String,
    val lastRun: WorkflowRunDto? = null
)

@Serializable
data class WorkflowRunDto(
    val id: Long,
    val status: String,
    val conclusion: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val triggeredBy: String? = null,
    val url: String
)

@Serializable
data class TriggerResponse(
    val success: Boolean,
    val message: String,
    val workflowId: String
)

// Gated Features DTOs
@Serializable
data class GatedFeatureDto(
    val key: String,
    val enabled: Boolean,
    val name: String,
    val description: String
)

@Serializable
data class SetFeatureRequest(
    val enabled: Boolean
)
