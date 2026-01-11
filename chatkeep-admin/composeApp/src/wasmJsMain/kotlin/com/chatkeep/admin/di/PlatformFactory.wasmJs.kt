package com.chatkeep.admin.di

import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.common.InMemoryTokenStorage

actual fun createPlatformDataStore(context: Any): Any {
    // WASM doesn't support DataStore - return dummy object
    return object {}
}

actual fun createPlatformTokenStorage(dataStore: Any): TokenStorage {
    // WASM doesn't use DataStore - returning in-memory storage
    return InMemoryTokenStorage()
}

actual fun getApiBaseUrl(): String {
    return "https://admin.chatmoderatorbot.ru"
}
