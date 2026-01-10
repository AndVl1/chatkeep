package com.chatkeep.admin.core.common

import platform.Foundation.NSBundle

/**
 * iOS implementation of BuildConfig.
 * Checks for DEBUG preprocessor macro via NSBundle.
 */
actual object BuildConfig {
    actual val isDebug: Boolean
        get() {
            // In iOS, check if the app is running in debug configuration
            // This checks for the "_IS_DEBUG" key injected by build config
            val bundle = NSBundle.mainBundle
            val debugFlag = bundle.objectForInfoDictionaryKey("IS_DEBUG") as? String
            return debugFlag == "1" || debugFlag?.lowercase() == "true"
        }
}
