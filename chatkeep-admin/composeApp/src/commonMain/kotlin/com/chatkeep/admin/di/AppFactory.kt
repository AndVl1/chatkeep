package com.chatkeep.admin.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.DefaultRootComponent
import com.chatkeep.admin.RootComponent
import com.chatkeep.admin.core.data.local.TokenStorage
import com.chatkeep.admin.core.data.remote.AdminApiService
import com.chatkeep.admin.core.data.remote.AdminApiServiceImpl
import com.chatkeep.admin.core.data.repository.*
import com.chatkeep.admin.core.domain.repository.*
import com.chatkeep.admin.core.domain.usecase.*
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
        AdminApiServiceImpl(httpClient, tokenStorage)
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
