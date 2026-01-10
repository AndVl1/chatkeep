package com.chatkeep.admin.core.data.mapper

import com.chatkeep.admin.core.data.remote.dto.AdminResponse
import com.chatkeep.admin.core.domain.model.Admin

fun AdminResponse.toDomain(): Admin {
    return Admin(
        id = id,
        firstName = firstName,
        lastName = lastName,
        username = username,
        photoUrl = photoUrl
    )
}
