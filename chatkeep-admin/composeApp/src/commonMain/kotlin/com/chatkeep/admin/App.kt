package com.chatkeep.admin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.chatkeep.admin.feature.settings.Theme
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import com.chatkeep.admin.core.ui.theme.AppTheme
import com.chatkeep.admin.feature.auth.ui.AuthScreen
import com.chatkeep.admin.feature.main.ui.MainScreen

@Composable
fun App(
    rootComponent: RootComponent,
    settingsRepository: SettingsRepository,
    onThemeChanged: (Boolean) -> Unit = {}
) {
    val settings by settingsRepository.settings.collectAsState()

    val darkTheme = when (settings.theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemInDarkTheme()
    }

    AppTheme(darkTheme = darkTheme, onThemeChanged = onThemeChanged) {
        RootContent(component = rootComponent)
    }
}

@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    val childStack by component.childStack.subscribeAsState()

    Children(
        stack = childStack,
        modifier = modifier.fillMaxSize(),
        animation = stackAnimation(fade())
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Auth -> AuthScreen(instance.component)
            is RootComponent.Child.Main -> MainScreen(instance.component)
        }
    }
}
