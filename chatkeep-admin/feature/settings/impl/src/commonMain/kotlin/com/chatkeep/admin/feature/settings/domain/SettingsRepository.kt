package com.chatkeep.admin.feature.settings.domain

import com.chatkeep.admin.feature.settings.Theme
import com.chatkeep.admin.feature.settings.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setTheme(theme: Theme)
    suspend fun setBaseUrl(url: String)
}
