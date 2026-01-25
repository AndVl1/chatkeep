package com.chatkeep.admin

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformHttpClient
import com.chatkeep.admin.di.createPlatformTokenStorage
import com.chatkeep.admin.di.getBaseUrlFromDataStore
import com.chatkeep.admin.core.common.BuildConfig
import kotlinx.coroutines.runBlocking
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    // Create DataStore first
    val dataStore = createPlatformDataStore(Unit)

    // Load base URL synchronously for iOS (no async initialization)
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
