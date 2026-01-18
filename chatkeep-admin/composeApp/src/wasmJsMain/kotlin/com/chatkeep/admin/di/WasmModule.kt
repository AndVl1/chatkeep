package com.chatkeep.admin.di

import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import com.chatkeep.admin.createSettingsRepository

/**
 * WASM-specific dependency module.
 * Manual DI module - ready for Metro migration when available.
 */
object WasmModule {

    fun provideTokenStorage(): TokenStorage {
        // WASM uses in-memory or localStorage-based token storage
        return createPlatformTokenStorage(Unit)
    }

    fun provideSettingsRepository(): SettingsRepository {
        // WASM uses in-memory settings repository
        return createSettingsRepository(Unit)
    }
}
