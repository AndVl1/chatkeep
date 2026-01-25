package com.chatkeep.admin.feature.chats

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.chats.Chat
import com.chatkeep.admin.feature.chats.data.ChatsRepositoryImpl
import com.chatkeep.admin.feature.chats.domain.GetChatsUseCase
import kotlinx.coroutines.launch

internal class DefaultChatsComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService,
    private val onNavigateToDetails: (chatId: Long, chatTitle: String) -> Unit
) : ChatsComponent, ComponentContext by componentContext {

    // Internal dependencies created within the component
    private val chatsRepository = ChatsRepositoryImpl(apiService)
    private val getChatsUseCase = GetChatsUseCase(chatsRepository)

    private val _state = MutableValue<ChatsComponent.ChatsState>(ChatsComponent.ChatsState.Loading)
    override val state: Value<ChatsComponent.ChatsState> = _state

    private val scope = componentScope()

    init {
        loadData()
    }

    override fun onRefresh() {
        loadData()
    }

    override fun onChatClick(chatId: Long, chatTitle: String) {
        onNavigateToDetails(chatId, chatTitle)
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
