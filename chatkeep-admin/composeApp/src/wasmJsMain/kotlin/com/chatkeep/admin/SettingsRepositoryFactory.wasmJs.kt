package com.chatkeep.admin

import com.chatkeep.admin.feature.settings.data.InMemorySettingsRepository
import com.chatkeep.admin.feature.settings.domain.SettingsRepository

actual fun createSettingsRepository(dataStore: Any): SettingsRepository {
    // WASM doesn't support DataStore - use in-memory repository (dataStore parameter ignored)
    return InMemorySettingsRepository()
}
