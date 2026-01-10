package com.chatkeep.admin.core.data.remote.dto

import kotlinx.serialization.Serializable

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
    val createdAt: String, // ISO-8601 string
    val triggeredBy: String
)

@Serializable
data class TriggerResponse(
    val runId: Long,
    val url: String
)
