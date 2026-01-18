package com.chatkeep.admin.feature.settings

data class UserSettings(
    val theme: Theme,
    val baseUrl: String = "https://admin.chatmoderatorbot.ru"
)

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}
