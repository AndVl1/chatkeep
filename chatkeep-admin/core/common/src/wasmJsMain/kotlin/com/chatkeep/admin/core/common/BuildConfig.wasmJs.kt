package com.chatkeep.admin.core.common

/**
 * WASM implementation of BuildConfig.
 * Checks JavaScript environment for debug flag.
 */
actual object BuildConfig {
    actual val isDebug: Boolean
        get() {
            // Check if running in development mode
            // In WASM, we check for a global debug flag set during build
            return js("typeof __DEBUG__ !== 'undefined' && __DEBUG__") as? Boolean ?: false
        }
}
