package com.chatkeep.admin.core.common

/**
 * Platform-specific build configuration.
 * Used to gate debug-only features like mock authentication.
 */
expect object BuildConfig {
    /**
     * Returns true if running in debug/development mode.
     * Mock authentication should ONLY work when this is true.
     */
    val isDebug: Boolean

    /**
     * Returns the base URL for auth backend.
     * This is environment-aware to prevent cross-origin issues.
     */
    val authBackendUrl: String
}
