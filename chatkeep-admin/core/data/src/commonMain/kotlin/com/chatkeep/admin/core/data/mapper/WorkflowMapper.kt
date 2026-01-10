package com.chatkeep.admin.core.data.mapper

import com.chatkeep.admin.core.data.remote.dto.WorkflowResponse
import com.chatkeep.admin.core.data.remote.dto.WorkflowRunDto
import com.chatkeep.admin.core.domain.model.Workflow
import com.chatkeep.admin.core.domain.model.WorkflowRun
import com.chatkeep.admin.core.domain.model.WorkflowStatus
import kotlinx.datetime.Instant

fun WorkflowResponse.toDomain(): Workflow {
    return Workflow(
        id = id,
        name = name,
        filename = filename,
        lastRun = lastRun?.toDomain()
    )
}

fun WorkflowRunDto.toDomain(): WorkflowRun {
    return WorkflowRun(
        id = id,
        status = status.toWorkflowStatus(),
        conclusion = conclusion,
        createdAt = Instant.parse(createdAt),
        triggeredBy = triggeredBy
    )
}

private fun String.toWorkflowStatus(): WorkflowStatus {
    return when (this.lowercase()) {
        "queued" -> WorkflowStatus.QUEUED
        "in_progress", "in progress" -> WorkflowStatus.IN_PROGRESS
        "completed" -> WorkflowStatus.COMPLETED
        else -> WorkflowStatus.UNKNOWN
    }
}
