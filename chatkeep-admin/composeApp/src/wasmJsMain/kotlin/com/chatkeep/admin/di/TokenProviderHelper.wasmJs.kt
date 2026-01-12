package com.chatkeep.admin.di

import com.chatkeep.admin.core.common.InMemoryTokenStorage
import com.chatkeep.admin.core.common.TokenStorage

actual fun createTokenProvider(tokenStorage: TokenStorage): () -> String? {
    // WASM only uses InMemoryTokenStorage which has synchronous access
    require(tokenStorage is InMemoryTokenStorage) {
        "WASM platform only supports InMemoryTokenStorage"
    }
    return { tokenStorage.getTokenSync() }
}
