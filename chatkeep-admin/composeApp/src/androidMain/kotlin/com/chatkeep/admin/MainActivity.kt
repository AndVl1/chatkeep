package com.chatkeep.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.chatkeep.admin.core.common.BuildConfig
import com.chatkeep.admin.core.common.DeepLinkData
import com.chatkeep.admin.core.network.createHttpClient
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformTokenStorage
import com.chatkeep.admin.di.getApiBaseUrl

class MainActivity : ComponentActivity() {

    private var rootComponent: RootComponent? = null

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
        rootComponent = appFactory.createRootComponent(
            componentContext = defaultComponentContext()
        )

        setContent {
            App(
                rootComponent = rootComponent!!,
                settingsRepository = appFactory.settingsRepository
            )
        }

        // Handle deeplink from initial intent
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deeplink from new intent (when app is already running)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return

        // Check if this is our auth callback deeplink
        if (uri.scheme == "chatkeep" && uri.host == "auth" && uri.path == "/callback") {
            val deepLinkData = DeepLinkData.fromUrl(uri.toString())
            if (deepLinkData != null) {
                rootComponent?.handleDeepLink(deepLinkData)
            }
        }
    }
}
