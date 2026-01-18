package com.chatkeep.admin.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.feature.settings.Theme
import com.chatkeep.admin.feature.settings.domain.SetBaseUrlUseCase
import com.chatkeep.admin.feature.settings.domain.SetThemeUseCase
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val settingsRepository: SettingsRepository,
    private val setThemeUseCase: SetThemeUseCase,
    private val setBaseUrlUseCase: SetBaseUrlUseCase,
    private val onLogout: () -> Unit
) : SettingsComponent, ComponentContext by componentContext {

    private val _state = MutableValue(
        SettingsComponent.SettingsState(
            theme = Theme.SYSTEM,
            baseUrl = "https://admin.chatmoderatorbot.ru",
            appVersion = "1.0.0"
        )
    )
    override val state: Value<SettingsComponent.SettingsState> = _state

    private val scope = componentScope()

    init {
        settingsRepository.settings
            .onEach { settings ->
                _state.value = SettingsComponent.SettingsState(
                    theme = settings.theme,
                    baseUrl = settings.baseUrl,
                    appVersion = "1.0.0"
                )
            }
            .launchIn(scope)
    }

    override fun onThemeChange(theme: Theme) {
        scope.launch {
            setThemeUseCase(theme)
        }
    }

    override fun onBaseUrlChange(url: String) {
        scope.launch {
            setBaseUrlUseCase(url)
        }
    }

    override fun onLogoutClick() {
        onLogout()
    }
}
