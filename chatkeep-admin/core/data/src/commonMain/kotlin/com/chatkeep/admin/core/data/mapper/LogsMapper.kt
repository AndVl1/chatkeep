package com.chatkeep.admin.core.data.mapper

import com.chatkeep.admin.core.data.remote.dto.LogsResponse
import com.chatkeep.admin.core.domain.model.LogsData
import kotlinx.datetime.Instant

fun LogsResponse.toDomain(): LogsData {
    return LogsData(
        lines = lines,
        timestamp = Instant.parse(timestamp)
    )
}
