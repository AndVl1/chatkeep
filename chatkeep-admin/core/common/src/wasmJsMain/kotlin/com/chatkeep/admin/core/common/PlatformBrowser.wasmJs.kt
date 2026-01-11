package com.chatkeep.admin.core.common

@JsFun("(url) => { window.open(url, '_blank'); }")
private external fun jsOpenWindow(url: String)

actual fun openInBrowser(url: String) {
    jsOpenWindow(url)
}
