package com.chatkeep.admin.core.common

import android.content.Context
import android.os.Build

class AndroidPlatformContext(val context: Context) : PlatformContext

actual fun createPlatformContext(): PlatformContext {
    throw UnsupportedOperationException("Use AndroidPlatformContext(context) instead")
}

actual fun getPlatformName(): String = "Android ${Build.VERSION.SDK_INT}"

actual fun createDataStorePath(context: PlatformContext): String {
    val androidContext = (context as AndroidPlatformContext).context
    return androidContext.filesDir.resolve("datastore").absolutePath
}
