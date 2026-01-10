package com.chatkeep.admin.core.domain.model

data class UserSettings(
    val theme: Theme
)

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}
