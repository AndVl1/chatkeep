package com.chatkeep.admin.feature.chatdetails

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.network.AdminApiService

/**
 * Factory function to create a ChatDetailsComponent.
 * This is the public API for creating chatdetails components from outside the impl module.
 */
fun createChatDetailsComponent(
    componentContext: ComponentContext,
    chatId: Long,
    chatTitle: String,
    apiService: AdminApiService,
    onBack: () -> Unit
): ChatDetailsComponent {
    return DefaultChatDetailsComponent(
        componentContext = componentContext,
        chatId = chatId,
        chatTitle = chatTitle,
        apiService = apiService,
        onBack = onBack
    )
}
