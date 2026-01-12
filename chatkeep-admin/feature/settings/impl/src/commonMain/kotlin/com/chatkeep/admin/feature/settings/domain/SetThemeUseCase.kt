package com.chatkeep.admin.feature.settings.domain

import com.chatkeep.admin.feature.settings.Theme

internal class SetThemeUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(theme: Theme) {
        settingsRepository.setTheme(theme)
    }
}
