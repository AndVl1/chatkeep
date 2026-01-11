package com.chatkeep.admin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.chatkeep.admin.core.network.createHttpClient
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformTokenStorage
import com.chatkeep.admin.di.getApiBaseUrl

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
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

    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App(
            rootComponent = rootComponent,
            settingsRepository = appFactory.settingsRepository
        )
    }
}
