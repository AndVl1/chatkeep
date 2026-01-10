package com.chatkeep.admin.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chatkeep.admin.core.domain.model.Theme
import com.chatkeep.admin.core.domain.model.UserSettings
import com.chatkeep.admin.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val themeKey = stringPreferencesKey("theme")

    private val _settings = MutableStateFlow(UserSettings(Theme.SYSTEM))
    override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    suspend fun init() {
        val theme = dataStore.data.map { preferences ->
            val themeString = preferences[themeKey] ?: "SYSTEM"
            Theme.valueOf(themeString)
        }.first()

        _settings.value = UserSettings(theme)
    }

    override suspend fun setTheme(theme: Theme) {
        dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
        _settings.value = _settings.value.copy(theme = theme)
    }
}
