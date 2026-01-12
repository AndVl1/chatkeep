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
expect fun createPlatformHttpClient(): HttpClient

expect fun createPlatformDataStore(context: Any): Any

expect fun createPlatformTokenStorage(dataStore: Any): TokenStorage

expect fun getApiBaseUrl(): String
