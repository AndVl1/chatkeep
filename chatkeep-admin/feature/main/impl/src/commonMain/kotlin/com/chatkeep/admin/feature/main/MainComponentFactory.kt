package com.chatkeep.admin.feature.main

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.settings.domain.SettingsRepository

/**
 * Factory function to create a MainComponent.
 * This is the public API for creating main components from outside the impl module.
 */
fun createMainComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService,
    settingsRepository: SettingsRepository,
    onLogout: () -> Unit
): MainComponent {
    return DefaultMainComponent(
        componentContext = componentContext,
        apiService = apiService,
        settingsRepository = settingsRepository,
        onLogout = onLogout
    )
}
