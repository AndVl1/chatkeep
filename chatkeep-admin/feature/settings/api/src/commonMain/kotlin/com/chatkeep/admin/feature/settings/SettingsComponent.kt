package com.chatkeep.admin.feature.settings

import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.feature.settings.Theme

interface SettingsComponent {
    val state: Value<SettingsState>
    fun onThemeChange(theme: Theme)
    fun onLogoutClick()

    data class SettingsState(
        val theme: Theme,
        val appVersion: String
    )
}
