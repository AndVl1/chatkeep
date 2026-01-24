package com.chatkeep.admin.feature.settings.domain

class SetBaseUrlUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(url: String) {
        settingsRepository.setBaseUrl(url)
    }
}
