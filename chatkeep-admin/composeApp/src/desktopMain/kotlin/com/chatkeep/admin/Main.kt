package com.chatkeep.admin

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformHttpClient
import com.chatkeep.admin.di.createPlatformTokenStorage
import com.chatkeep.admin.di.getApiBaseUrl

fun main() = application {
    val lifecycle = LifecycleRegistry()

    // Create platform dependencies
    val httpClient = createPlatformHttpClient()
    val dataStore = createPlatformDataStore(Unit)
    val tokenStorage = createPlatformTokenStorage(dataStore)
    val baseUrl = getApiBaseUrl()

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

    Window(
        onCloseRequest = ::exitApplication,
        title = "ChatKeep Admin"
    ) {
        LifecycleController(lifecycle, windowState = window)

        App(
            rootComponent = rootComponent,
            settingsRepository = appFactory.settingsRepository
        )
    }
}
