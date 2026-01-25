package com.chatkeep.admin.core.common

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Android implementation of BuildConfig.
 * Checks if the application is debuggable via ApplicationInfo.
 * Determines API URL based on application ID (flavor).
 */
actual object BuildConfig {
    private var context: Context? = null

    /**
     * Initialize with application context.
     * Must be called before accessing isDebug.
     */
    fun init(appContext: Context) {
        context = appContext.applicationContext
        // Also initialize browser context
        initBrowserContext(appContext)
    }

    actual val isDebug: Boolean
        get() {
            val ctx = context ?: return false
            return (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }

    actual val DEFAULT_BASE_URL: String
        get() {
            val ctx = context
            // staging flavor has .test suffix in applicationId
            return if (ctx != null && ctx.packageName.endsWith(".test")) {
                "https://chatmodtest.ru/api"
            } else {
                "https://api.chatmoderatorbot.ru"
            }
        }

    actual val AUTH_DOMAIN: String
        get() {
            val ctx = context
            // staging flavor has .test suffix in applicationId
            return if (ctx != null && ctx.packageName.endsWith(".test")) {
                "https://admin.chatmodtest.ru"
            } else {
                "https://admin.chatmoderatorbot.ru"
            }
        }
}
