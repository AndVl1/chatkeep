package com.chatkeep.admin.feature.main

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.feature.dashboard.DashboardComponent
import com.chatkeep.admin.feature.chats.ChatsComponent
import com.chatkeep.admin.feature.deploy.DeployComponent
import com.chatkeep.admin.feature.logs.LogsComponent
import com.chatkeep.admin.feature.settings.SettingsComponent
import kotlinx.serialization.Serializable

interface MainComponent {
    val childStack: Value<ChildStack<*, Child>>

    fun onTabSelect(tab: Tab)

    sealed class Child {
        data class Dashboard(val component: DashboardComponent) : Child()
        data class Chats(val component: ChatsComponent) : Child()
        data class Deploy(val component: DeployComponent) : Child()
        data class Logs(val component: LogsComponent) : Child()
        data class Settings(val component: SettingsComponent) : Child()
    }

    @Serializable
    sealed class Config {
        @Serializable data object Dashboard : Config()
        @Serializable data object Chats : Config()
        @Serializable data object Deploy : Config()
        @Serializable data object Logs : Config()
        @Serializable data object Settings : Config()
    }

    enum class Tab {
        DASHBOARD,
        CHATS,
        DEPLOY,
        LOGS,
        SETTINGS
    }
}
