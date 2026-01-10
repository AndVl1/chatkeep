package com.chatkeep.admin.core.common

import java.io.File

class DesktopPlatformContext : PlatformContext

actual fun createPlatformContext(): PlatformContext = DesktopPlatformContext()

actual fun getPlatformName(): String =
    "${System.getProperty("os.name")} ${System.getProperty("os.version")}"

actual fun createDataStorePath(context: PlatformContext): String {
    val home = System.getProperty("user.home")
    return File(home, ".chatkeep-admin/datastore").absolutePath
}
