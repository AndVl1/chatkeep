package com.chatkeep.admin.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chatkeep.admin.feature.settings.Theme
import com.chatkeep.admin.feature.settings.UserSettings
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * DataStore-based settings repository for iOS.
 * Settings are persisted using DataStore.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val themeKey = stringPreferencesKey("theme")
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _settings = MutableStateFlow(UserSettings(Theme.SYSTEM))
    override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    init {
        // Load settings from DataStore
        dataStore.data
            .map { preferences ->
                val themeString = preferences[themeKey] ?: "SYSTEM"
                val baseUrl = preferences[baseUrlKey] ?: "https://admin.chatmoderatorbot.ru"
                UserSettings(
                    theme = Theme.valueOf(themeString),
                    baseUrl = baseUrl
                )
            }
            .onEach { _settings.value = it }
            .launchIn(scope)
    }

    override suspend fun setTheme(theme: Theme) {
        dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
        _settings.value = _settings.value.copy(theme = theme)
    }

    override suspend fun setBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[baseUrlKey] = url
        }
        _settings.value = _settings.value.copy(baseUrl = url)
    }
}
