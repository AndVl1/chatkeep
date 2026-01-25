package com.chatkeep.admin.core.common

/**
 * WASM implementation of BuildConfig.
 * TODO: Implement debug flag check for WASM when needed
 */
actual object BuildConfig {
    actual val isDebug: Boolean = false
    actual val DEFAULT_BASE_URL: String = BuildConfigGenerated.API_BASE_URL
    actual val AUTH_DOMAIN: String = BuildConfigGenerated.AUTH_DOMAIN
}
