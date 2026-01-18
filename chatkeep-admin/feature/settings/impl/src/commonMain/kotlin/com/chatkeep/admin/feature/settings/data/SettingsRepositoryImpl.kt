package com.chatkeep.admin.feature.settings.data

import com.chatkeep.admin.feature.settings.Theme
import com.chatkeep.admin.feature.settings.UserSettings
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory settings repository (used by WASM and as fallback).
 * Settings are not persisted between sessions.
 */
class InMemorySettingsRepository : SettingsRepository {
    private val _settings = MutableStateFlow(UserSettings(Theme.SYSTEM))
    override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    override suspend fun setTheme(theme: Theme) {
        _settings.value = _settings.value.copy(theme = theme)
    }

    override suspend fun setBaseUrl(url: String) {
        _settings.value = _settings.value.copy(baseUrl = url)
    }
}
