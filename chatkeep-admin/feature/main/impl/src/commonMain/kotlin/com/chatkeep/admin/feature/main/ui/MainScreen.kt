package com.chatkeep.admin.feature.main.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.chatkeep.admin.feature.main.MainComponent
import com.chatkeep.admin.feature.dashboard.ui.DashboardScreen
import com.chatkeep.admin.feature.chats.ui.ChatsScreen
import com.chatkeep.admin.feature.deploy.ui.DeployScreen
import com.chatkeep.admin.feature.settings.ui.SettingsScreen

@Composable
fun MainScreen(component: MainComponent, modifier: Modifier = Modifier) {
    val childStack by component.childStack.subscribeAsState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (!isCompact) {
                NavigationRail(childStack.active.configuration, component::onTabSelect)
            }

            Column(modifier = Modifier.weight(1f)) {
                Children(
                    stack = childStack,
                    modifier = Modifier.weight(1f)
                ) { child ->
                    when (val instance = child.instance) {
                        is MainComponent.Child.Dashboard -> DashboardScreen(instance.component)
                        is MainComponent.Child.Chats -> ChatsScreen(instance.component)
                        is MainComponent.Child.Deploy -> DeployScreen(instance.component)
                        is MainComponent.Child.Settings -> SettingsScreen(instance.component)
                    }
                }

                if (isCompact) {
                    BottomNavigationBar(childStack.active.configuration, component::onTabSelect)
                }
            }
        }
    }
}

@Composable
private fun NavigationRail(
    activeConfig: Any,
    onTabSelect: (MainComponent.Tab) -> Unit
) {
    NavigationRail {
        Spacer(modifier = Modifier.height(8.dp))

        NavigationRailItem(
            selected = activeConfig is MainComponent.Config.Dashboard,
            onClick = { onTabSelect(MainComponent.Tab.DASHBOARD) },
            icon = { Text("📊") },
            label = { Text("Dashboard") }
        )

        NavigationRailItem(
            selected = activeConfig is MainComponent.Config.Chats,
            onClick = { onTabSelect(MainComponent.Tab.CHATS) },
            icon = { Text("💬") },
            label = { Text("Chats") }
        )

        NavigationRailItem(
            selected = activeConfig is MainComponent.Config.Deploy,
            onClick = { onTabSelect(MainComponent.Tab.DEPLOY) },
            icon = { Text("🚀") },
            label = { Text("Deploy") }
        )

        NavigationRailItem(
            selected = activeConfig is MainComponent.Config.Settings,
            onClick = { onTabSelect(MainComponent.Tab.SETTINGS) },
            icon = { Text("⚙️") },
            label = { Text("Settings") }
        )
    }
}

@Composable
private fun BottomNavigationBar(
    activeConfig: Any,
    onTabSelect: (MainComponent.Tab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = activeConfig is MainComponent.Config.Dashboard,
            onClick = { onTabSelect(MainComponent.Tab.DASHBOARD) },
            icon = { Text("📊") },
            label = { Text("Dashboard") }
        )

        NavigationBarItem(
            selected = activeConfig is MainComponent.Config.Chats,
            onClick = { onTabSelect(MainComponent.Tab.CHATS) },
            icon = { Text("💬") },
            label = { Text("Chats") }
        )

        NavigationBarItem(
            selected = activeConfig is MainComponent.Config.Deploy,
            onClick = { onTabSelect(MainComponent.Tab.DEPLOY) },
            icon = { Text("🚀") },
            label = { Text("Deploy") }
        )

        NavigationBarItem(
            selected = activeConfig is MainComponent.Config.Settings,
            onClick = { onTabSelect(MainComponent.Tab.SETTINGS) },
            icon = { Text("⚙️") },
            label = { Text("Settings") }
        )
    }
}
