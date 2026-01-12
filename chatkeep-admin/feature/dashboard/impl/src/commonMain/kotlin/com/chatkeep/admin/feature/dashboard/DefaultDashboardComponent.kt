package com.chatkeep.admin.feature.dashboard

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.dashboard.data.repository.ActionsRepositoryImpl
import com.chatkeep.admin.feature.dashboard.data.repository.DashboardRepositoryImpl
import com.chatkeep.admin.feature.dashboard.domain.usecase.GetDashboardUseCase
import com.chatkeep.admin.feature.dashboard.domain.usecase.RestartBotUseCase
import kotlinx.coroutines.launch

internal class DefaultDashboardComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService
) : DashboardComponent, ComponentContext by componentContext {

    // Internal dependencies created within the component
    private val dashboardRepository = DashboardRepositoryImpl(apiService)
    private val actionsRepository = ActionsRepositoryImpl(apiService)
    private val getDashboardUseCase = GetDashboardUseCase(dashboardRepository)
    private val restartBotUseCase = RestartBotUseCase(actionsRepository)

    private val _state = MutableValue<DashboardComponent.DashboardState>(DashboardComponent.DashboardState.Loading)
    override val state: Value<DashboardComponent.DashboardState> = _state

    private val scope = componentScope()

    init {
        loadData()
    }

    override fun onRefresh() {
        loadData()
    }

    override fun onRestartClick() {
        val currentState = _state.value
        if (currentState is DashboardComponent.DashboardState.Success) {
            _state.value = currentState.copy(showRestartDialog = true)
        }
    }

    override fun onConfirmRestart() {
        val currentState = _state.value
        if (currentState is DashboardComponent.DashboardState.Success) {
            _state.value = currentState.copy(
                showRestartDialog = false,
                isRestarting = true
            )
            performRestart()
        }
    }

    override fun onDismissDialog() {
        val currentState = _state.value
        if (currentState is DashboardComponent.DashboardState.Success) {
            _state.value = currentState.copy(showRestartDialog = false)
        }
    }

    private fun loadData() {
        scope.launch {
            _state.value = DashboardComponent.DashboardState.Loading
            when (val result = getDashboardUseCase()) {
                is AppResult.Success -> {
                    _state.value = DashboardComponent.DashboardState.Success(result.data)
                }
                is AppResult.Error -> {
                    _state.value = DashboardComponent.DashboardState.Error(result.message)
                }
                is AppResult.Loading -> {
                    _state.value = DashboardComponent.DashboardState.Loading
                }
            }
        }
    }

    private fun performRestart() {
        scope.launch {
            when (val result = restartBotUseCase()) {
                is AppResult.Success -> {
                    // Reload dashboard after restart
                    loadData()
                }
                is AppResult.Error -> {
                    val currentState = _state.value
                    if (currentState is DashboardComponent.DashboardState.Success) {
                        _state.value = currentState.copy(isRestarting = false)
                    }
                    _state.value = DashboardComponent.DashboardState.Error(result.message)
                }
                is AppResult.Loading -> {
                    // Keep showing restarting state
                }
            }
        }
    }
}
