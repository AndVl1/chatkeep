package com.chatkeep.admin.feature.settings.data

import com.chatkeep.admin.feature.settings.Theme
import com.chatkeep.admin.feature.settings.UserSettings
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory settings repository for WASM platform.
 * Settings are not persisted between sessions.
 */
internal actual class SettingsRepositoryImpl() : SettingsRepository {

    private val _settings = MutableStateFlow(UserSettings(Theme.SYSTEM))
    actual override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    actual override suspend fun setTheme(theme: Theme) {
        _settings.value = _settings.value.copy(theme = theme)
    }
}
