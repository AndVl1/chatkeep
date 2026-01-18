package com.chatkeep.admin.di

/**
 * Common dependency module for all platforms.
 * Contains shared dependencies that work across Android, iOS, Desktop, and WASM.
 * Manual DI module - ready for Metro migration when available.
 */
object CommonModule {

    fun provideBaseUrl(): String = "https://admin.chatmoderatorbot.ru"
}
