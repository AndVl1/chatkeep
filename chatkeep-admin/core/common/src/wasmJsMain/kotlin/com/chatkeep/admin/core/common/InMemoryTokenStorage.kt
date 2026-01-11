package com.chatkeep.admin.core.common

/**
 * Simple in-memory token storage for WASM platform.
 * Note: This does not persist tokens between sessions.
 * TODO: Implement browser localStorage persistence when needed.
 */
class InMemoryTokenStorage : TokenStorage {
    private var token: String? = null

    override suspend fun saveToken(token: String) {
        this.token = token
    }

    override suspend fun getToken(): String? {
        return token
    }

    override suspend fun clearToken() {
        token = null
    }

    /**
     * Synchronous getter for WASM platform where runBlocking is not available.
     * Safe to use because InMemoryTokenStorage doesn't perform async operations.
     */
    fun getTokenSync(): String? {
        return token
    }
}
