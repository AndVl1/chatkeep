package com.chatkeep.admin.core.common

@JsFun("(url) => { window.open(url, '_blank'); return true; }")
private external fun jsOpenWindow(url: String)

actual fun openInBrowser(url: String) {
    jsOpenWindow(url)
}
