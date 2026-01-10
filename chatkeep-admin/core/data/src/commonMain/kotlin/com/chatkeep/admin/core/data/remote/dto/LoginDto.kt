package com.chatkeep.admin.core.data.remote.dto

import kotlinx.serialization.Serializable

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
