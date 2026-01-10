package com.chatkeep.admin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.chatkeep.admin.di.createPlatformHttpClient
import com.chatkeep.admin.di.getApiBaseUrl
import com.chatkeep.admin.core.common.InMemoryTokenStorage
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.core.network.AdminApiServiceImpl
import com.chatkeep.admin.feature.auth.domain.repository.AuthRepository
import com.chatkeep.admin.feature.auth.data.repository.AuthRepositoryImpl
import com.chatkeep.admin.feature.dashboard.domain.repository.DashboardRepository
import com.chatkeep.admin.feature.dashboard.domain.repository.ActionsRepository
import com.chatkeep.admin.feature.dashboard.data.repository.DashboardRepositoryImpl
import com.chatkeep.admin.feature.dashboard.data.repository.ActionsRepositoryImpl
import com.chatkeep.admin.feature.chats.domain.ChatsRepository
import com.chatkeep.admin.feature.chats.data.ChatsRepositoryImpl
import com.chatkeep.admin.feature.deploy.domain.WorkflowsRepository
import com.chatkeep.admin.feature.deploy.data.WorkflowsRepositoryImpl
import com.chatkeep.admin.feature.logs.domain.LogsRepository
import com.chatkeep.admin.feature.logs.data.LogsRepositoryImpl
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import com.chatkeep.admin.feature.settings.data.SettingsRepositoryImpl
import com.chatkeep.admin.feature.auth.domain.usecase.LoginUseCase
import com.chatkeep.admin.feature.dashboard.domain.usecase.GetDashboardUseCase
import com.chatkeep.admin.feature.dashboard.domain.usecase.RestartBotUseCase
import com.chatkeep.admin.feature.chats.domain.GetChatsUseCase
import com.chatkeep.admin.feature.deploy.domain.usecase.GetWorkflowsUseCase
import com.chatkeep.admin.feature.deploy.domain.usecase.TriggerWorkflowUseCase
import com.chatkeep.admin.feature.settings.domain.SetThemeUseCase

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()

    // Create platform dependencies
    val httpClient = createPlatformHttpClient()
    val tokenStorage = InMemoryTokenStorage()
    val baseUrl = getApiBaseUrl()

    // API Service
    val apiService: AdminApiService = AdminApiServiceImpl(httpClient) {
        runCatching { kotlinx.coroutines.runBlocking { tokenStorage.getToken() } }.getOrNull()
    }

    // Repositories
    val authRepository: AuthRepository = AuthRepositoryImpl(apiService, tokenStorage)
    val dashboardRepository: DashboardRepository = DashboardRepositoryImpl(apiService)
    val chatsRepository: ChatsRepository = ChatsRepositoryImpl(apiService)
    val workflowsRepository: WorkflowsRepository = WorkflowsRepositoryImpl(apiService)
    val logsRepository: LogsRepository = LogsRepositoryImpl(apiService)
    val actionsRepository: ActionsRepository = ActionsRepositoryImpl(apiService)
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl()

    // Use Cases
    val loginUseCase = LoginUseCase(authRepository)
    val getDashboardUseCase = GetDashboardUseCase(dashboardRepository)
    val restartBotUseCase = RestartBotUseCase(actionsRepository)
    val getChatsUseCase = GetChatsUseCase(chatsRepository)
    val getWorkflowsUseCase = GetWorkflowsUseCase(workflowsRepository)
    val triggerWorkflowUseCase = TriggerWorkflowUseCase(workflowsRepository)
    val setThemeUseCase = SetThemeUseCase(settingsRepository)

    // Create root component
    val rootComponent = DefaultRootComponent(
        componentContext = DefaultComponentContext(lifecycle),
        authRepository = authRepository,
        getDashboardUseCase = getDashboardUseCase,
        restartBotUseCase = restartBotUseCase,
        getChatsUseCase = getChatsUseCase,
        getWorkflowsUseCase = getWorkflowsUseCase,
        triggerWorkflowUseCase = triggerWorkflowUseCase,
        settingsRepository = settingsRepository,
        setThemeUseCase = setThemeUseCase,
        loginUseCase = loginUseCase
    )

    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App(
            rootComponent = rootComponent,
            settingsRepository = settingsRepository
        )
    }
}
