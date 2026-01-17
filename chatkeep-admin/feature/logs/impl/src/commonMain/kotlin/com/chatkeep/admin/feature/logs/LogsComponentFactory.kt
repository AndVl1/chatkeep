package com.chatkeep.admin.feature.logs

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.logs.data.LogsRepositoryImpl
import com.chatkeep.admin.feature.logs.domain.GetLogsUseCase

/**
 * Factory function to create a LogsComponent.
 * This is the public API for creating logs components from outside the impl module.
 */
fun createLogsComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService
): LogsComponent {
    val repository = LogsRepositoryImpl(apiService)
    val getLogsUseCase = GetLogsUseCase(repository)

    return DefaultLogsComponent(
        componentContext = componentContext,
        getLogsUseCase = getLogsUseCase
    )
}
