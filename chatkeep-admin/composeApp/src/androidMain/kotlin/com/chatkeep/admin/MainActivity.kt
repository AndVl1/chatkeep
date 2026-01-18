package com.chatkeep.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.arkivanov.decompose.defaultComponentContext
import com.chatkeep.admin.core.common.BuildConfig
import com.chatkeep.admin.core.common.DeepLinkData
import com.chatkeep.admin.di.AndroidAppGraph

class MainActivity : ComponentActivity() {

    private var rootComponent: RootComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize BuildConfig for debug mode detection
        BuildConfig.init(applicationContext)

        // Create dependency graph
        val graph = AndroidAppGraph(this)

        // Create root component with injected dependencies
        rootComponent = graph.createRootComponent(
            componentContext = defaultComponentContext()
        )

        setContent {
            App(
                rootComponent = rootComponent!!,
                settingsRepository = graph.settingsRepository,
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
