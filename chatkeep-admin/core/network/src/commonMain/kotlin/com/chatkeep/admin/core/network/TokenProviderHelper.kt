package com.chatkeep.admin.core.network

import com.chatkeep.admin.core.common.TokenStorage

/**
 * Creates a synchronous token provider from a TokenStorage.
 * Platform implementations handle the suspend-to-sync conversion appropriately.
 */
expect fun createTokenProvider(tokenStorage: TokenStorage): () -> String?
