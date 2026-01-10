package com.chatkeep.admin

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.feature.auth.AuthState
import com.chatkeep.admin.feature.auth.domain.repository.AuthRepository
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import com.chatkeep.admin.feature.auth.domain.usecase.LoginUseCase
import com.chatkeep.admin.feature.dashboard.domain.usecase.GetDashboardUseCase
import com.chatkeep.admin.feature.dashboard.domain.usecase.RestartBotUseCase
import com.chatkeep.admin.feature.chats.domain.GetChatsUseCase
import com.chatkeep.admin.feature.deploy.domain.usecase.GetWorkflowsUseCase
import com.chatkeep.admin.feature.deploy.domain.usecase.TriggerWorkflowUseCase
import com.chatkeep.admin.feature.settings.domain.SetThemeUseCase
import com.chatkeep.admin.feature.auth.AuthComponent
import com.chatkeep.admin.feature.auth.DefaultAuthComponent
import com.chatkeep.admin.feature.main.DefaultMainComponent
import com.chatkeep.admin.feature.main.MainComponent
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        data class Auth(val component: AuthComponent) : Child()
        data class Main(val component: MainComponent) : Child()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val getDashboardUseCase: GetDashboardUseCase,
    private val restartBotUseCase: RestartBotUseCase,
    private val getChatsUseCase: GetChatsUseCase,
    private val getWorkflowsUseCase: GetWorkflowsUseCase,
    private val triggerWorkflowUseCase: TriggerWorkflowUseCase,
    private val settingsRepository: SettingsRepository,
    private val setThemeUseCase: SetThemeUseCase,
    private val loginUseCase: LoginUseCase
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()
    private val scope = componentScope()

    override val childStack: Value<ChildStack<Config, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Splash,
            handleBackButton = true,
            childFactory = ::createChild
        )

    init {
        // Observe auth state and navigate accordingly
        scope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        // Stay on current screen
                    }
                    is AuthState.NotAuthenticated -> {
                        navigation.replaceAll(Config.Auth)
                    }
                    is AuthState.Authenticated -> {
                        navigation.replaceAll(Config.Main)
                    }
                }
            }
        }

        // Trigger session restore on start
        scope.launch {
            authRepository.restoreSession()
        }
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object Splash : Config()

        @Serializable
        data object Auth : Config()

        @Serializable
        data object Main : Config()
    }

    private fun createChild(
        config: Config,
        context: ComponentContext
    ): RootComponent.Child = when (config) {
        Config.Splash -> RootComponent.Child.Auth(createAuthComponent(context))
        Config.Auth -> RootComponent.Child.Auth(createAuthComponent(context))
        Config.Main -> RootComponent.Child.Main(createMainComponent(context))
    }

    private fun createAuthComponent(context: ComponentContext): AuthComponent {
        return DefaultAuthComponent(
            componentContext = context,
            loginUseCase = loginUseCase,
            onSuccess = { /* Auth state change will trigger navigation */ }
        )
    }

    private fun createMainComponent(context: ComponentContext): MainComponent {
        return DefaultMainComponent(
            componentContext = context,
            getDashboardUseCase = getDashboardUseCase,
            restartBotUseCase = restartBotUseCase,
            getChatsUseCase = getChatsUseCase,
            getWorkflowsUseCase = getWorkflowsUseCase,
            triggerWorkflowUseCase = triggerWorkflowUseCase,
            settingsRepository = settingsRepository,
            setThemeUseCase = setThemeUseCase,
            onLogout = {
                scope.launch {
                    authRepository.logout()
                }
            }
        )
    }
}
