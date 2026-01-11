package com.chatkeep.admin.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.common.InMemoryTokenStorage
import com.chatkeep.admin.core.network.createHttpClient
import io.ktor.client.*
import io.ktor.client.engine.js.*

actual fun createPlatformHttpClient(): HttpClient = createHttpClient(Js.create())

actual fun createPlatformDataStore(context: Any): DataStore<Preferences> {
    error("DataStore is not supported on WASM platform")
}

actual fun createPlatformTokenStorage(dataStore: DataStore<Preferences>): TokenStorage {
    // Note: We don't use dataStore on WASM, returning in-memory storage
    return InMemoryTokenStorage()
}

actual fun getApiBaseUrl(): String {
    return "https://admin.chatmoderatorbot.ru"
}
