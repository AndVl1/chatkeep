package com.chatkeep.admin.core.common

import java.awt.Desktop
import java.net.URI

actual fun openInBrowser(url: String) {
    if (Desktop.isDesktopSupported()) {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(URI(url))
        }
    }
}
