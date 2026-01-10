package com.chatkeep.admin.feature.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.feature.chats.DefaultChatsComponent
import com.chatkeep.admin.feature.dashboard.DefaultDashboardComponent
import com.chatkeep.admin.feature.deploy.DefaultDeployComponent
import com.chatkeep.admin.feature.settings.DefaultSettingsComponent
import com.chatkeep.admin.core.domain.usecase.*
import com.chatkeep.admin.core.domain.repository.SettingsRepository

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val getDashboardUseCase: GetDashboardUseCase,
    private val restartBotUseCase: RestartBotUseCase,
    private val getChatsUseCase: GetChatsUseCase,
    private val getWorkflowsUseCase: GetWorkflowsUseCase,
    private val triggerWorkflowUseCase: TriggerWorkflowUseCase,
    private val settingsRepository: SettingsRepository,
    private val setThemeUseCase: SetThemeUseCase,
    private val onLogout: () -> Unit
) : MainComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<MainComponent.Config>()

    override val childStack: Value<ChildStack<*, MainComponent.Child>> =
        childStack(
            source = navigation,
            serializer = MainComponent.Config.serializer(),
            initialConfiguration = MainComponent.Config.Dashboard,
            handleBackButton = false,
            childFactory = ::createChild
        )

    private fun createChild(
        config: MainComponent.Config,
        context: ComponentContext
    ): MainComponent.Child = when (config) {
        MainComponent.Config.Dashboard -> MainComponent.Child.Dashboard(
            DefaultDashboardComponent(
                componentContext = context,
                getDashboardUseCase = getDashboardUseCase,
                restartBotUseCase = restartBotUseCase
            )
        )
        MainComponent.Config.Chats -> MainComponent.Child.Chats(
            DefaultChatsComponent(
                componentContext = context,
                getChatsUseCase = getChatsUseCase
            )
        )
        MainComponent.Config.Deploy -> MainComponent.Child.Deploy(
            DefaultDeployComponent(
                componentContext = context,
                getWorkflowsUseCase = getWorkflowsUseCase,
                triggerWorkflowUseCase = triggerWorkflowUseCase
            )
        )
        MainComponent.Config.Settings -> MainComponent.Child.Settings(
            DefaultSettingsComponent(
                componentContext = context,
                settingsRepository = settingsRepository,
                setThemeUseCase = setThemeUseCase,
                onLogout = onLogout
            )
        )
    }

    override fun onTabSelect(tab: MainComponent.Tab) {
        val config = when (tab) {
            MainComponent.Tab.DASHBOARD -> MainComponent.Config.Dashboard
            MainComponent.Tab.CHATS -> MainComponent.Config.Chats
            MainComponent.Tab.DEPLOY -> MainComponent.Config.Deploy
            MainComponent.Tab.SETTINGS -> MainComponent.Config.Settings
        }

        navigation.replaceAll(config)
    }
}
