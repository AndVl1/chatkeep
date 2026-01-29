package com.chatkeep.admin.feature.settings

import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.feature.settings.Theme

interface SettingsComponent {
    val state: Value<SettingsState>
    fun onThemeChange(theme: Theme)
    fun onBaseUrlChange(url: String)
    fun onLogoutClick()
    fun onShowThemeDialog()
    fun onDismissThemeDialog()
    fun onShowBaseUrlDialog()
    fun onDismissBaseUrlDialog()

    data class SettingsState(
        val theme: Theme,
        val baseUrl: String,
        val appVersion: String,
        val showThemeDialog: Boolean = false,
        val showBaseUrlDialog: Boolean = false
    )
}
