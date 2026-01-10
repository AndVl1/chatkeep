package com.chatkeep.admin.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chatkeep.admin.core.common.IosPlatformContext
import com.chatkeep.admin.core.common.createDataStorePath
import com.chatkeep.admin.core.data.local.DataStoreTokenStorage
import com.chatkeep.admin.core.data.local.TokenStorage
import com.chatkeep.admin.core.network.createHttpClient
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath

@OptIn(ExperimentalForeignApi::class)
actual fun createPlatformHttpClient(): HttpClient = createHttpClient(Darwin.create())

actual fun createPlatformDataStore(context: Any): DataStore<Preferences> {
    val platformContext = IosPlatformContext()
    val path = createDataStorePath(platformContext)
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { "$path/chatkeep_admin.preferences_pb".toPath() }
    )
}

actual fun createPlatformTokenStorage(dataStore: DataStore<Preferences>): TokenStorage {
    return DataStoreTokenStorage(dataStore)
}

actual fun getApiBaseUrl(): String {
    return "https://api.chatmoderatorbot.ru"
}
