package com.chatkeep.admin.core.common

/**
 * Desktop (JVM) implementation of BuildConfig.
 * Checks system property or environment variable for debug mode.
 */
actual object BuildConfig {
    actual val isDebug: Boolean
        get() {
            // Check system property first, then environment variable
            val sysProp = System.getProperty("chatkeep.debug", "false")
            val envVar = System.getenv("CHATKEEP_DEBUG") ?: "false"

            return sysProp.toBoolean() || envVar.toBoolean()
        }
}
