package com.chatkeep.admin.feature.settings.data

import com.chatkeep.admin.feature.settings.Theme
import com.chatkeep.admin.feature.settings.UserSettings
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

internal expect class SettingsRepositoryImpl : SettingsRepository {
    override val settings: StateFlow<UserSettings>
    override suspend fun setTheme(theme: Theme)
}
