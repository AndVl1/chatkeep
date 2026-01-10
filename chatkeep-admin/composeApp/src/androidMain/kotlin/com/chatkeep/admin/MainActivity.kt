package com.chatkeep.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.chatkeep.admin.core.common.BuildConfig
import com.chatkeep.admin.core.network.createHttpClient
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformTokenStorage
import com.chatkeep.admin.di.getApiBaseUrl

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize BuildConfig for debug mode detection
        BuildConfig.init(applicationContext)

        // Create platform dependencies
        val baseUrl = getApiBaseUrl()
        val httpClient = createHttpClient(baseUrl)
        val dataStore = createPlatformDataStore(this)
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
            componentContext = defaultComponentContext()
        )

        setContent {
            App(
                rootComponent = rootComponent,
                settingsRepository = appFactory.settingsRepository
            )
        }
    }
}
