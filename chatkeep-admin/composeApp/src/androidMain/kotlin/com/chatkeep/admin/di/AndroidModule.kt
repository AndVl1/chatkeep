package com.chatkeep.admin.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.data.local.DataStoreTokenStorage
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import com.chatkeep.admin.createSettingsRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chatkeep_admin_prefs"
)

/**
 * Android-specific dependency module.
 * Manual DI module - ready for Metro migration when available.
 */
object AndroidModule {

    fun provideDataStore(context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    fun provideTokenStorage(
        dataStore: DataStore<Preferences>
    ): TokenStorage = DataStoreTokenStorage(dataStore)

    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>
    ): SettingsRepository = createSettingsRepository(dataStore)
}
