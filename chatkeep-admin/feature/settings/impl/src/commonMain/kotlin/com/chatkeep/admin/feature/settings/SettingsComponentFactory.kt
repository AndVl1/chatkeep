package com.chatkeep.admin.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.feature.settings.data.InMemorySettingsRepository
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import com.chatkeep.admin.feature.settings.domain.SetThemeUseCase

/**
 * Factory function to create a SettingsComponent.
 * This version accepts a repository, useful when you need platform-specific dependencies.
 */
fun createSettingsComponent(
    componentContext: ComponentContext,
    settingsRepository: SettingsRepository,
    onLogout: () -> Unit
): SettingsComponent {
    val setThemeUseCase = SetThemeUseCase(settingsRepository)

    return DefaultSettingsComponent(
        componentContext = componentContext,
        settingsRepository = settingsRepository,
        setThemeUseCase = setThemeUseCase,
        onLogout = onLogout
    )
}

/**
 * Factory function for WASM (no external dependencies needed).
 * This creates an in-memory settings repository.
 */
fun createSettingsComponent(
    componentContext: ComponentContext,
    onLogout: () -> Unit
): SettingsComponent {
    val settingsRepository = InMemorySettingsRepository()
    return createSettingsComponent(componentContext, settingsRepository, onLogout)
}
