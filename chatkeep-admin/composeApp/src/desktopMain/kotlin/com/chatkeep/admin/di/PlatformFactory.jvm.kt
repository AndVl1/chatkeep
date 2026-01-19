package com.chatkeep.admin.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chatkeep.admin.core.common.DesktopPlatformContext
import com.chatkeep.admin.core.common.createDataStorePath
import com.chatkeep.admin.core.data.local.DataStoreTokenStorage
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.network.createHttpClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

actual fun createPlatformHttpClient(baseUrl: String): HttpClient = createHttpClient(baseUrl)

actual fun createPlatformDataStore(context: Any): Any {
    val platformContext = DesktopPlatformContext()
    val path = createDataStorePath(platformContext)
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { "$path/chatkeep_admin.preferences_pb".toPath() }
    )
}

actual fun createPlatformTokenStorage(dataStore: Any): TokenStorage {
    @Suppress("UNCHECKED_CAST")
    return DataStoreTokenStorage(dataStore as DataStore<Preferences>)
}

actual suspend fun getBaseUrlFromDataStore(dataStore: Any): String? {
    @Suppress("UNCHECKED_CAST")
    val ds = dataStore as DataStore<Preferences>
    val preferences = ds.data.first()
    val baseUrlKey = stringPreferencesKey("base_url")
    return preferences[baseUrlKey] ?: System.getenv("API_BASE_URL")
}
