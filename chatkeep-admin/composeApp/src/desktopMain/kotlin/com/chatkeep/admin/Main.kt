package com.chatkeep.admin

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.chatkeep.admin.core.network.createHttpClient
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformTokenStorage
import com.chatkeep.admin.di.getApiBaseUrl

fun main() = application {
    val lifecycle = LifecycleRegistry()

    // Create platform dependencies
    val baseUrl = getApiBaseUrl()
    val httpClient = createHttpClient(baseUrl)
    val dataStore = createPlatformDataStore(Unit)
    val tokenStorage = createPlatformTokenStorage(dataStore)

    // Create app factory
    val appFactory = AppFactory(
        httpClient = httpClient,
        baseUrl = baseUrl,
        tokenStorage = tokenStorage,
        dataStore = dataStore
    )

    // Create root component
    val rootComponent = appFactory.createRootComponent(
        componentContext = DefaultComponentContext(lifecycle)
    )

    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "ChatKeep Admin",
        state = windowState
    ) {
        LifecycleController(lifecycle, windowState)

        App(
            rootComponent = rootComponent,
            settingsRepository = appFactory.settingsRepository
        )
    }
}
