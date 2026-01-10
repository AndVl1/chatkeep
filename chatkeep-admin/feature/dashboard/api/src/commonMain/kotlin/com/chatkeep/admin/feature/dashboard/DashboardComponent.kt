package com.chatkeep.admin.feature.dashboard

import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.domain.model.DashboardInfo

interface DashboardComponent {
    val state: Value<DashboardState>
    fun onRefresh()
    fun onRestartClick()
    fun onConfirmRestart()
    fun onDismissDialog()

    sealed class DashboardState {
        data object Loading : DashboardState()
        data class Success(
            val dashboard: DashboardInfo,
            val isRestarting: Boolean = false,
            val showRestartDialog: Boolean = false
        ) : DashboardState()
        data class Error(val message: String) : DashboardState()
    }
}
