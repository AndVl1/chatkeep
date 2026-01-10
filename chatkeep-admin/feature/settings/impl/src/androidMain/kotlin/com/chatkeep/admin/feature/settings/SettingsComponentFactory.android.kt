package com.chatkeep.admin.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chatkeep.admin.feature.settings.data.SettingsRepositoryImpl

actual fun createSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepositoryImpl {
    return SettingsRepositoryImpl(dataStore)
}
