package com.chatkeep.admin.feature.deploy

import kotlinx.datetime.Instant

data class Workflow(
    val id: String,
    val name: String,
    val filename: String,
    val lastRun: WorkflowRun?
)

data class WorkflowRun(
    val id: Long,
    val status: WorkflowStatus,
    val conclusion: String?,
    val createdAt: Instant,
    val triggeredBy: String
)

enum class WorkflowStatus {
    QUEUED,
    IN_PROGRESS,
    COMPLETED,
    UNKNOWN
}

data class WorkflowTriggerResult(
    val runId: Long,
    val url: String
)
