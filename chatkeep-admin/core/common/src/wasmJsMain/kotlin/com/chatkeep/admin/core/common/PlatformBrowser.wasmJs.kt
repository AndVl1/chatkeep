package com.chatkeep.admin.core.common

@JsFun("(url) => { window.open(url, 'chatkeep-auth', 'width=600,height=700,popup=yes'); }")
private external fun jsOpenWindow(url: String)

actual fun openInBrowser(url: String) {
    jsOpenWindow(url)
}
