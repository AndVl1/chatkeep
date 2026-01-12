package com.chatkeep.admin.feature.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.chats.createChatsComponent
import com.chatkeep.admin.feature.dashboard.createDashboardComponent
import com.chatkeep.admin.feature.deploy.createDeployComponent
import com.chatkeep.admin.feature.settings.createSettingsComponent
import com.chatkeep.admin.feature.settings.domain.SettingsRepository

internal class DefaultMainComponent(
    componentContext: ComponentContext,
    private val apiService: AdminApiService,
    private val settingsRepository: SettingsRepository,
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
            createDashboardComponent(
                componentContext = context,
                apiService = apiService
            )
        )
        MainComponent.Config.Chats -> MainComponent.Child.Chats(
            createChatsComponent(
                componentContext = context,
                apiService = apiService
            )
        )
        MainComponent.Config.Deploy -> MainComponent.Child.Deploy(
            createDeployComponent(
                componentContext = context,
                apiService = apiService
            )
        )
        MainComponent.Config.Settings -> MainComponent.Child.Settings(
            createSettingsComponent(
                componentContext = context,
                settingsRepository = settingsRepository,
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
