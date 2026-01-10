package com.chatkeep.admin.core.domain.model

data class Admin(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val photoUrl: String?
)
