package com.chatkeep.admin.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.feature.settings.data.SettingsRepositoryImpl
import com.chatkeep.admin.feature.settings.domain.SetThemeUseCase

/**
 * Factory function to create a SettingsComponent.
 * This is the public API for creating settings components from outside the impl module.
 */
fun createSettingsComponent(
    componentContext: ComponentContext,
    dataStore: DataStore<Preferences>,
    onLogout: () -> Unit
): SettingsComponent {
    val settingsRepository = createSettingsRepository(dataStore)
    val setThemeUseCase = SetThemeUseCase(settingsRepository)

    return DefaultSettingsComponent(
        componentContext = componentContext,
        settingsRepository = settingsRepository,
        setThemeUseCase = setThemeUseCase,
        onLogout = onLogout
    )
}

/**
 * Platform-specific factory for creating SettingsRepositoryImpl.
 */
expect fun createSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepositoryImpl
