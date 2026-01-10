package com.chatkeep.admin.core.data.remote.dto

import kotlinx.serialization.Serializable

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
    val deployedAt: String, // ISO-8601 string
    val imageVersion: String
)

@Serializable
data class QuickStatsDto(
    val totalChats: Int,
    val messagesToday: Int,
    val messagesYesterday: Int
)
