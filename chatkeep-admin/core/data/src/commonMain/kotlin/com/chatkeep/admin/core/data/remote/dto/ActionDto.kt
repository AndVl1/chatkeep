package com.chatkeep.admin.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ActionResponse(
    val success: Boolean,
    val message: String
)
