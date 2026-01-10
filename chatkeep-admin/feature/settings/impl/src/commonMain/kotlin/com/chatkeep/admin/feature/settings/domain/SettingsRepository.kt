package com.chatkeep.admin.feature.settings.domain

import com.chatkeep.admin.feature.settings.Theme
import com.chatkeep.admin.feature.settings.UserSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<UserSettings>
    suspend fun setTheme(theme: Theme)
}
