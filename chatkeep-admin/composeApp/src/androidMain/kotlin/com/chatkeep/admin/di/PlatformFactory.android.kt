package com.chatkeep.admin.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.preferencesDataStore
import com.chatkeep.admin.core.common.AndroidPlatformContext
import com.chatkeep.admin.core.common.createDataStorePath
import com.chatkeep.admin.core.data.local.DataStoreTokenStorage
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.network.createHttpClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chatkeep_admin_prefs")

actual fun createPlatformHttpClient(): HttpClient = createHttpClient()

actual fun createPlatformDataStore(context: Any): Any {
    require(context is Context) { "Expected Android Context" }
    return context.dataStore
}

actual fun createPlatformTokenStorage(dataStore: Any): TokenStorage {
    @Suppress("UNCHECKED_CAST")
    return DataStoreTokenStorage(dataStore as DataStore<Preferences>)
}

actual fun getApiBaseUrl(): String {
    return "https://admin.chatmoderatorbot.ru"  // TODO: Move to BuildConfig
}
