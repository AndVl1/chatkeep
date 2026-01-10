package com.chatkeep.admin.core.domain.usecase

import com.chatkeep.admin.core.domain.model.Theme
import com.chatkeep.admin.core.domain.repository.SettingsRepository

class SetThemeUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(theme: Theme) {
        settingsRepository.setTheme(theme)
    }
}
