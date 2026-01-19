package com.chatkeep.admin.di

import com.chatkeep.admin.core.common.TokenStorage
import io.ktor.client.*

/**
 * Platform-specific factory methods.
 * These must be implemented for each platform (Android, iOS, Desktop, WASM).
 *
 * Note: DataStore types are replaced with Any to avoid WASM compatibility issues.
 * Platform implementations should cast as needed.
 */
expect fun createPlatformHttpClient(baseUrl: String): HttpClient

expect fun createPlatformDataStore(context: Any): Any

expect fun createPlatformTokenStorage(dataStore: Any): TokenStorage

/**
 * Reads base URL from DataStore preferences.
 * Returns null if not found (first run).
 */
expect suspend fun getBaseUrlFromDataStore(dataStore: Any): String?
