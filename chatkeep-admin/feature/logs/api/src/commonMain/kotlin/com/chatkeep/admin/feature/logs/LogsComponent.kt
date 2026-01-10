package com.chatkeep.admin.feature.logs

import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.domain.model.LogsData

interface LogsComponent {
    val state: Value<LogsState>
    fun onRefresh()

    sealed class LogsState {
        data object Loading : LogsState()
        data class Success(val logs: LogsData) : LogsState()
        data class Error(val message: String) : LogsState()
    }
}
