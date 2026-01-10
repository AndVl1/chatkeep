package com.chatkeep.admin.core.data.mapper

import com.chatkeep.admin.core.data.remote.dto.LoginRequest
import com.chatkeep.admin.core.domain.repository.TelegramLoginData

fun TelegramLoginData.toRequest(): LoginRequest {
    return LoginRequest(
        id = id,
        firstName = firstName,
        lastName = lastName,
        username = username,
        photoUrl = photoUrl,
        authDate = authDate,
        hash = hash
    )
}
