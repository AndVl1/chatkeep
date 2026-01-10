package com.chatkeep.admin.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminResponse(
    val id: Long,
    val firstName: String,
    val lastName: String? = null,
    val username: String? = null,
    val photoUrl: String? = null
)
