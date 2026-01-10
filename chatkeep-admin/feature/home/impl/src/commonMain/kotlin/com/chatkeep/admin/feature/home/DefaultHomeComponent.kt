package com.chatkeep.admin.feature.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.network.ApiService
import kotlinx.coroutines.launch

class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val apiService: ApiService,
    private val onNavigateToDetails: (itemId: String) -> Unit
) : HomeComponent, ComponentContext by componentContext {

    private val _state = MutableValue<HomeState>(HomeState.Loading)
    override val state: Value<HomeState> = _state

    private val scope = componentScope()

    init {
        loadData()
    }

    private fun loadData() {
        scope.launch {
            _state.value = HomeState.Loading
            when (val result = apiService.getItems()) {
                is AppResult.Success -> {
                    _state.value = HomeState.Success(
                        result.data.map { apiItem ->
                            HomeItem(
                                id = apiItem.id,
                                title = apiItem.title,
                                description = apiItem.description,
                                timestamp = apiItem.timestamp
                            )
                        }
                    )
                }
                is AppResult.Error -> {
                    _state.value = HomeState.Error(result.message)
                }
                is AppResult.Loading -> {
                    _state.value = HomeState.Loading
                }
            }
        }
    }

    override fun onItemClick(item: HomeItem) {
        onNavigateToDetails(item.id)
    }

    override fun onRefresh() {
        loadData()
    }
}
