package com.chatkeep.admin.feature.auth

data class TelegramLoginData(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val photoUrl: String?,
    val authDate: Long,
    val hash: String
)
