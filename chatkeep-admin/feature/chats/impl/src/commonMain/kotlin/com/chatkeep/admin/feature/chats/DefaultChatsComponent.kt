package com.chatkeep.admin.feature.chats

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.domain.model.Chat
import com.chatkeep.admin.core.domain.usecase.GetChatsUseCase
import kotlinx.coroutines.launch

class DefaultChatsComponent(
    componentContext: ComponentContext,
    private val getChatsUseCase: GetChatsUseCase
) : ChatsComponent, ComponentContext by componentContext {

    private val _state = MutableValue<ChatsComponent.ChatsState>(ChatsComponent.ChatsState.Loading)
    override val state: Value<ChatsComponent.ChatsState> = _state

    private val scope = componentScope()

    init {
        loadData()
    }

    override fun onRefresh() {
        loadData()
    }

    override fun onChatClick(chat: Chat) {
        // Future: navigate to chat details
    }

    private fun loadData() {
        scope.launch {
            _state.value = ChatsComponent.ChatsState.Loading
            when (val result = getChatsUseCase()) {
                is AppResult.Success -> {
                    // Sort by messages today descending
                    val sorted = result.data.sortedByDescending { it.messagesToday }
                    _state.value = ChatsComponent.ChatsState.Success(sorted)
                }
                is AppResult.Error -> {
                    _state.value = ChatsComponent.ChatsState.Error(result.message)
                }
                is AppResult.Loading -> {
                    _state.value = ChatsComponent.ChatsState.Loading
                }
            }
        }
    }
}
