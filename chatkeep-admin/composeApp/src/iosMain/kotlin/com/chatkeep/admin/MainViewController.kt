package com.chatkeep.admin

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformHttpClient
import com.chatkeep.admin.di.createPlatformTokenStorage
import com.chatkeep.admin.di.getApiBaseUrl
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
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
        componentContext = DefaultComponentContext(
            lifecycle = ApplicationLifecycle()
        )
    )

    return ComposeUIViewController {
        App(
            rootComponent = rootComponent,
            settingsRepository = appFactory.settingsRepository
        )
    }
}
