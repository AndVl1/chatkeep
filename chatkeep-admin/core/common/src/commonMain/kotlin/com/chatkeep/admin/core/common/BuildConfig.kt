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
     * Default base URL for API calls.
     * Can be overridden at build time via gradle property: -Papi.base.url=<url>
     */
    val DEFAULT_BASE_URL: String

    /**
     * Auth domain for Telegram Login Widget page.
     * Derived from API base URL (api.domain -> admin.domain).
     */
    val AUTH_DOMAIN: String
}
