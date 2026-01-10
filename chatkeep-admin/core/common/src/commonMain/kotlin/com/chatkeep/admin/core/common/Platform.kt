package com.chatkeep.admin.core.common

interface PlatformContext

expect fun createPlatformContext(): PlatformContext

expect fun getPlatformName(): String

expect fun createDataStorePath(context: PlatformContext): String
