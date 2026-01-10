package com.chatkeep.admin.core.network

import kotlinx.serialization.Serializable

// Auth DTOs
@Serializable
data class LoginRequest(
    val id: Long,
    val firstName: String,
    val lastName: String? = null,
    val username: String? = null,
    val photoUrl: String? = null,
    val authDate: Long,
    val hash: String
)

@Serializable
data class LoginResponse(
    val token: String,
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
    val commitSha: String,
    val deployedAt: String,
    val imageVersion: String
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
    val chatTitle: String,
    val messagesToday: Int,
    val messagesYesterday: Int
)

// Logs DTOs
@Serializable
data class LogsResponse(
    val lines: List<String>,
    val timestamp: String
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
    val triggeredBy: String
)

@Serializable
data class TriggerResponse(
    val runId: Long,
    val url: String
)
