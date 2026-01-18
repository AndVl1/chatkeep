package com.chatkeep.admin.di

import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.common.InMemoryTokenStorage
import com.chatkeep.admin.core.network.createHttpClient
import io.ktor.client.*
import io.ktor.client.engine.js.*

actual fun createPlatformHttpClient(baseUrl: String): HttpClient = createHttpClient(baseUrl)

actual fun createPlatformDataStore(context: Any): Any {
    // WASM doesn't support DataStore - return dummy object
    return object {}
}

actual fun createPlatformTokenStorage(dataStore: Any): TokenStorage {
    // WASM doesn't use DataStore - returning in-memory storage
    return InMemoryTokenStorage()
}

actual suspend fun getBaseUrlFromDataStore(dataStore: Any): String? {
    // WASM doesn't support DataStore - return null (will use default)
    return null
}
