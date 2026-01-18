package com.chatkeep.admin.core.network

import com.chatkeep.admin.core.common.TokenStorage
import kotlinx.coroutines.runBlocking

actual fun createTokenProvider(tokenStorage: TokenStorage): () -> String? {
    return {
        runCatching {
            runBlocking { tokenStorage.getToken() }
        }.getOrNull()
    }
}
