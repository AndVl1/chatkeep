package com.chatkeep.admin.core.common

class WasmPlatformContext : PlatformContext

actual fun createPlatformContext(): PlatformContext = WasmPlatformContext()

actual fun getPlatformName(): String = "Web (WASM)"

actual fun createDataStorePath(context: PlatformContext): String {
    return "chatkeep-admin-datastore"
}
