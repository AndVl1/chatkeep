package com.chatkeep.admin.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesDataStore(private val dataStore: DataStore<Preferences>) {

    private val userNameKey = stringPreferencesKey("user_name")
    private val themeKey = stringPreferencesKey("theme")

    val userName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[userNameKey]
    }

    val theme: Flow<String> = dataStore.data.map { preferences ->
        preferences[themeKey] ?: "system"
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[userNameKey] = name
        }
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[themeKey] = theme
        }
    }
}
