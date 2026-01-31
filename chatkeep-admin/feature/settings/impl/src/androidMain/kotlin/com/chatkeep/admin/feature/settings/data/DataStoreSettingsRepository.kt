package com.chatkeep.admin.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chatkeep.admin.core.common.BuildConfig
import com.chatkeep.admin.feature.settings.Theme
import com.chatkeep.admin.feature.settings.UserSettings
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-based settings repository for Android.
 * Settings are persisted using DataStore.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val themeKey = stringPreferencesKey("theme")
    private val baseUrlKey = stringPreferencesKey("base_url")

    override val settings: Flow<UserSettings> = dataStore.data.map { preferences ->
        val themeString = preferences[themeKey] ?: "SYSTEM"
        val baseUrl = preferences[baseUrlKey] ?: BuildConfig.DEFAULT_BASE_URL
        UserSettings(
            theme = Theme.valueOf(themeString),
            baseUrl = baseUrl
        )
    }

    override suspend fun setTheme(theme: Theme) {
        dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    override suspend fun setBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[baseUrlKey] = url
        }
    }
}
