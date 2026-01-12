package com.chatkeep.admin.feature.settings

data class UserSettings(
    val theme: Theme
)

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}
