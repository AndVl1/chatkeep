package com.chatkeep.admin.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.data.local.DataStoreTokenStorage
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import com.chatkeep.admin.createSettingsRepository

/**
 * Desktop-specific dependency module.
 * Manual DI module - ready for Metro migration when available.
 */
object DesktopModule {

    fun provideDataStore(): DataStore<Preferences> {
        // Desktop DataStore created via createPlatformDataStore
        return createPlatformDataStore(Unit) as DataStore<Preferences>
    }

    fun provideTokenStorage(
        dataStore: DataStore<Preferences>
    ): TokenStorage = DataStoreTokenStorage(dataStore)

    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>
    ): SettingsRepository = createSettingsRepository(dataStore)
}
