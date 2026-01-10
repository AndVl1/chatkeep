package com.chatkeep.admin.feature.chats

import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.domain.model.Chat

interface ChatsComponent {
    val state: Value<ChatsState>
    fun onRefresh()
    fun onChatClick(chat: Chat)

    sealed class ChatsState {
        data object Loading : ChatsState()
        data class Success(val chats: List<Chat>) : ChatsState()
        data class Error(val message: String) : ChatsState()
    }
}
