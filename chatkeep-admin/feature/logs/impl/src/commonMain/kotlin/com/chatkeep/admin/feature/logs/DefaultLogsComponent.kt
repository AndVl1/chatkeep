package com.chatkeep.admin.feature.logs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.feature.logs.domain.GetLogsUseCase
import kotlinx.coroutines.launch

internal class DefaultLogsComponent(
    componentContext: ComponentContext,
    private val getLogsUseCase: GetLogsUseCase
) : LogsComponent, ComponentContext by componentContext {

    private val _state = MutableValue<LogsComponent.LogsState>(LogsComponent.LogsState.Loading)
    override val state: Value<LogsComponent.LogsState> = _state

    private val scope = componentScope()

    init {
        loadData()
    }

    override fun onRefresh() {
        loadData()
    }

    private fun loadData() {
        scope.launch {
            _state.value = LogsComponent.LogsState.Loading
            when (val result = getLogsUseCase()) {
                is AppResult.Success -> {
                    _state.value = LogsComponent.LogsState.Success(result.data)
                }
                is AppResult.Error -> {
                    _state.value = LogsComponent.LogsState.Error(result.message)
                }
                is AppResult.Loading -> {
                    _state.value = LogsComponent.LogsState.Loading
                }
            }
        }
    }
}
