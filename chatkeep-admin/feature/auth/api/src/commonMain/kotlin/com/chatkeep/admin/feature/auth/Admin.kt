package com.chatkeep.admin.feature.auth

data class Admin(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val photoUrl: String?
)
