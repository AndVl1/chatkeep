package com.chatkeep.admin.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chatkeep.admin.core.common.TokenStorage
import io.ktor.client.*

/**
 * Platform-specific factory methods.
 * These must be implemented for each platform (Android, iOS, Desktop, WASM).
 */
expect fun createPlatformDataStore(context: Any): DataStore<Preferences>

expect fun createPlatformTokenStorage(dataStore: DataStore<Preferences>): TokenStorage

expect fun getApiBaseUrl(): String
