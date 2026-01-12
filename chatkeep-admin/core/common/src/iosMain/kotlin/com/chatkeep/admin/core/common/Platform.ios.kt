package com.chatkeep.admin.core.common

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

class IosPlatformContext : PlatformContext

actual fun createPlatformContext(): PlatformContext = IosPlatformContext()

actual fun getPlatformName(): String = "iOS"

@OptIn(ExperimentalForeignApi::class)
actual fun createDataStorePath(context: PlatformContext): String {
    val documentDir = NSFileManager.defaultManager.URLForDirectory(
        NSDocumentDirectory,
        NSUserDomainMask,
        null,
        false,
        null
    )
    return "${documentDir?.path}/datastore"
}
