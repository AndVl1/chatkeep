package com.chatkeep.admin.core.data.mapper

import com.chatkeep.admin.core.data.remote.dto.DashboardResponse
import com.chatkeep.admin.core.data.remote.dto.DeployInfoDto
import com.chatkeep.admin.core.data.remote.dto.QuickStatsDto
import com.chatkeep.admin.core.data.remote.dto.ServiceStatusDto
import com.chatkeep.admin.core.domain.model.DashboardInfo
import com.chatkeep.admin.core.domain.model.DeployInfo
import com.chatkeep.admin.core.domain.model.QuickStats
import com.chatkeep.admin.core.domain.model.ServiceStatus
import kotlinx.datetime.Instant

fun DashboardResponse.toDomain(): DashboardInfo {
    return DashboardInfo(
        serviceStatus = serviceStatus.toDomain(),
        deployInfo = deployInfo.toDomain(),
        quickStats = quickStats.toDomain()
    )
}

fun ServiceStatusDto.toDomain(): ServiceStatus {
    return ServiceStatus(
        running = running,
        uptime = uptime
    )
}

fun DeployInfoDto.toDomain(): DeployInfo {
    return DeployInfo(
        commitSha = commitSha,
        deployedAt = Instant.parse(deployedAt),
        imageVersion = imageVersion
    )
}

fun QuickStatsDto.toDomain(): QuickStats {
    return QuickStats(
        totalChats = totalChats,
        messagesToday = messagesToday,
        messagesYesterday = messagesYesterday
    )
}
