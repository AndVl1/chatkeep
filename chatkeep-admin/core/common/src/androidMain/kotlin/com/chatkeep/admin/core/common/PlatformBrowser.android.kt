package com.chatkeep.admin.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri

// Store application context for browser usage
private var appContext: Context? = null

/**
 * Initialize with application context.
 * Called automatically when BuildConfig.init() is called in MainActivity.
 */
internal fun initBrowserContext(context: Context) {
    appContext = context.applicationContext
}

actual fun openInBrowser(url: String) {
    val context = appContext
        ?: throw IllegalStateException("Browser context not initialized. Call BuildConfig.init() first.")

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
