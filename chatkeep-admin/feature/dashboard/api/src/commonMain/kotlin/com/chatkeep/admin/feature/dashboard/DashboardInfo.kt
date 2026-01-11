package com.chatkeep.admin.feature.dashboard

import kotlinx.datetime.Instant

data class DashboardInfo(
    val serviceStatus: ServiceStatus,
    val deployInfo: DeployInfo,
    val quickStats: QuickStats
)

data class ServiceStatus(
    val running: Boolean,
    val uptime: Long // seconds
)

data class DeployInfo(
    val commitSha: String?,
    val deployedAt: Instant?,
    val imageVersion: String?
)

data class QuickStats(
    val totalChats: Int,
    val messagesToday: Int,
    val messagesYesterday: Int
) {
    val trend: Trend
        get() = when {
            messagesToday > messagesYesterday -> Trend.UP
            messagesToday < messagesYesterday -> Trend.DOWN
            else -> Trend.SAME
        }
}

enum class Trend { UP, DOWN, SAME }
