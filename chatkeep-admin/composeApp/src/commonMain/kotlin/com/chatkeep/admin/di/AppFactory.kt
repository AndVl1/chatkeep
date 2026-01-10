package com.chatkeep.admin.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.DefaultRootComponent
import com.chatkeep.admin.RootComponent
import com.chatkeep.admin.core.common.TokenStorage
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
import io.ktor.client.*

/**
 * Simple dependency factory for creating the app's dependency graph.
 * This is a lightweight alternative to a full DI framework.
 */
class AppFactory(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val dataStore: DataStore<Preferences>
) {
    // API Service
    val apiService: AdminApiService by lazy {
        AdminApiServiceImpl(httpClient) {
            runCatching { kotlinx.coroutines.runBlocking { tokenStorage.getToken() } }.getOrNull()
        }
    }

    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, tokenStorage)
    }

    val dashboardRepository: DashboardRepository by lazy {
        DashboardRepositoryImpl(apiService)
    }

    val chatsRepository: ChatsRepository by lazy {
        ChatsRepositoryImpl(apiService)
    }

    val workflowsRepository: WorkflowsRepository by lazy {
        WorkflowsRepositoryImpl(apiService)
    }

    val logsRepository: LogsRepository by lazy {
        LogsRepositoryImpl(apiService)
    }

    val actionsRepository: ActionsRepository by lazy {
        ActionsRepositoryImpl(apiService)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(dataStore)
    }

    // Use Cases
    val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository)
    }

    val getDashboardUseCase: GetDashboardUseCase by lazy {
        GetDashboardUseCase(dashboardRepository)
    }

    val restartBotUseCase: RestartBotUseCase by lazy {
        RestartBotUseCase(actionsRepository)
    }

    val getChatsUseCase: GetChatsUseCase by lazy {
        GetChatsUseCase(chatsRepository)
    }

    val getWorkflowsUseCase: GetWorkflowsUseCase by lazy {
        GetWorkflowsUseCase(workflowsRepository)
    }

    val triggerWorkflowUseCase: TriggerWorkflowUseCase by lazy {
        TriggerWorkflowUseCase(workflowsRepository)
    }

    val setThemeUseCase: SetThemeUseCase by lazy {
        SetThemeUseCase(settingsRepository)
    }

    /**
     * Creates the root component with all dependencies wired up.
     */
    fun createRootComponent(componentContext: ComponentContext): RootComponent {
        return DefaultRootComponent(
            componentContext = componentContext,
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
    }
}
