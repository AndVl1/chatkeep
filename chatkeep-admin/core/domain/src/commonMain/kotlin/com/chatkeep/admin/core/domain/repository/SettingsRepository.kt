package com.chatkeep.admin.core.domain.repository

import com.chatkeep.admin.core.domain.model.Theme
import com.chatkeep.admin.core.domain.model.UserSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<UserSettings>
    suspend fun setTheme(theme: Theme)
}
