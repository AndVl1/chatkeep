package com.chatkeep.admin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chatkeep.admin.feature.settings.data.DataStoreSettingsRepository
import com.chatkeep.admin.feature.settings.domain.SettingsRepository

actual fun createSettingsRepository(dataStore: Any): SettingsRepository {
    @Suppress("UNCHECKED_CAST")
    return DataStoreSettingsRepository(dataStore as DataStore<Preferences>)
}
