package com.chatkeep.admin.feature.chatdetails

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.chatdetails.data.ChatDetailsRepositoryImpl
import com.chatkeep.admin.feature.chatdetails.domain.GetChatFeaturesUseCase
import com.chatkeep.admin.feature.chatdetails.domain.SetChatFeatureUseCase
import kotlinx.coroutines.launch

internal class DefaultChatDetailsComponent(
    componentContext: ComponentContext,
    private val chatId: Long,
    private val chatTitle: String,
    apiService: AdminApiService,
    private val onBack: () -> Unit
) : ChatDetailsComponent, ComponentContext by componentContext {

    // Internal dependencies
    private val repository = ChatDetailsRepositoryImpl(apiService)
    private val getChatFeaturesUseCase = GetChatFeaturesUseCase(repository)
    private val setChatFeatureUseCase = SetChatFeatureUseCase(repository)

    private val _state = MutableValue<ChatDetailsState>(ChatDetailsState.Loading)
    override val state: Value<ChatDetailsState> = _state

    private val scope = componentScope()

    init {
        loadFeatures()
    }

    override fun onFeatureToggle(featureKey: String, enabled: Boolean) {
        scope.launch {
            when (val result = setChatFeatureUseCase.execute(chatId, featureKey, enabled)) {
                is AppResult.Success -> {
                    // Update the feature in current state
                    val currentState = _state.value
                    if (currentState is ChatDetailsState.Success) {
                        val updatedFeatures = currentState.features.map { feature ->
                            if (feature.key == featureKey) {
                                result.data
                            } else {
                                feature
                            }
                        }
                        _state.value = currentState.copy(features = updatedFeatures)
                    }
                }
                is AppResult.Error -> {
                    // Keep current state and show error
                    // TODO: Add error handling UI (toast/snackbar)
                }
                is AppResult.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    override fun onBackClick() {
        onBack()
    }

    private fun loadFeatures() {
        scope.launch {
            _state.value = ChatDetailsState.Loading
            when (val result = getChatFeaturesUseCase.execute(chatId)) {
                is AppResult.Success -> {
                    _state.value = ChatDetailsState.Success(
                        chatId = chatId,
                        chatTitle = chatTitle,
                        features = result.data
                    )
                }
                is AppResult.Error -> {
                    _state.value = ChatDetailsState.Error(result.message)
                }
                is AppResult.Loading -> {
                    _state.value = ChatDetailsState.Loading
                }
            }
        }
    }
}
