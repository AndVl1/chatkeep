package com.chatkeep.admin.feature.chats

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.network.AdminApiService

/**
 * Factory function to create a ChatsComponent.
 * This is the public API for creating chats components from outside the impl module.
 */
fun createChatsComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService,
    onNavigateToDetails: (chatId: Long, chatTitle: String) -> Unit
): ChatsComponent {
    return DefaultChatsComponent(
        componentContext = componentContext,
        apiService = apiService,
        onNavigateToDetails = onNavigateToDetails
    )
}
