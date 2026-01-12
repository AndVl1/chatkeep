package com.chatkeep.admin.core.common

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openInBrowser(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
