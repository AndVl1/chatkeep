package com.chatkeep.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import com.chatkeep.admin.core.common.BuildConfig
import com.chatkeep.admin.core.common.DeepLinkData
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformHttpClient
import com.chatkeep.admin.di.createPlatformTokenStorage
import com.chatkeep.admin.di.getBaseUrlFromDataStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var rootComponent: RootComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize BuildConfig for debug mode detection
        BuildConfig.init(applicationContext)

        // Create DataStore first
        val dataStore = createPlatformDataStore(this)

        // Load base URL asynchronously and initialize app
        lifecycleScope.launch {
            val baseUrl = getBaseUrlFromDataStore(dataStore) ?: "https://admin.chatmoderatorbot.ru"
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
            rootComponent = appFactory.createRootComponent(
                componentContext = defaultComponentContext()
            )

            setContent {
                App(
                    rootComponent = rootComponent!!,
                    settingsRepository = appFactory.settingsRepository,
                    onThemeChanged = { darkTheme ->
                        // Update system bar appearance
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = !darkTheme
                            isAppearanceLightNavigationBars = !darkTheme
                        }
                    }
                )
            }

            // Handle deeplink from initial intent - deferred to ensure component is ready
            window.decorView.post {
                handleDeepLink(intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deeplink from new intent (when app is already running)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return

        Log.d(TAG, "Handling deeplink: $uri")

        // Check if this is our auth callback deeplink
        if (uri.scheme == "chatkeep" && uri.host == "auth" && uri.path == "/callback") {
            try {
                val deepLinkData = DeepLinkData.fromUrl(uri.toString())
                if (deepLinkData != null) {
                    Log.d(TAG, "Deeplink parsed successfully, forwarding to root component")
                    rootComponent?.handleDeepLink(deepLinkData)
                } else {
                    Log.e(TAG, "Failed to parse deeplink: missing required parameters in $uri")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing deeplink: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "Ignoring non-auth deeplink: $uri")
        }
    }

    companion object {
        private const val TAG = "ChatKeepAuth"
    }
}
