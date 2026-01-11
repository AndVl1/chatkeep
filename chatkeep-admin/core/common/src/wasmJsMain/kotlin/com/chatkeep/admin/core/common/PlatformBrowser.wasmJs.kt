package com.chatkeep.admin.core.common

import kotlinx.browser.window

actual fun openInBrowser(url: String) {
    window.open(url, "_blank")
}
