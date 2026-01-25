package com.chatkeep.admin

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformHttpClient
import com.chatkeep.admin.di.createPlatformTokenStorage
import com.chatkeep.admin.di.getBaseUrlFromDataStore
import com.chatkeep.admin.core.common.BuildConfig
import kotlinx.coroutines.runBlocking

fun main() = application {
    val lifecycle = LifecycleRegistry()

    // Create DataStore first
    val dataStore = createPlatformDataStore(Unit)

    // Load base URL synchronously for Desktop
    val baseUrl = runBlocking {
        getBaseUrlFromDataStore(dataStore) ?: BuildConfig.DEFAULT_BASE_URL
    }

    val httpClient = createPlatformHttpClient(baseUrl)
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
