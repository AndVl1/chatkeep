package com.chatkeep.admin.core.common

/**
 * Helper function to get hostname from browser window.
 * Must be at top-level for WASM js() call restrictions.
 */
private fun getHostname(): String = js("window.location.hostname")

/**
 * WASM implementation of BuildConfig.
 * Auth backend URL is determined dynamically from window.location.hostname
 */
actual object BuildConfig {
    actual val isDebug: Boolean = false

    actual val authBackendUrl: String
        get() {
            val hostname = getHostname()
            return when {
                hostname.contains("localhost") -> "http://localhost:8080"
                hostname.contains("chatmodtest") -> "https://admin.chatmodtest.ru"
                else -> "https://admin.chatmoderatorbot.ru"  // production
            }
        }
}
