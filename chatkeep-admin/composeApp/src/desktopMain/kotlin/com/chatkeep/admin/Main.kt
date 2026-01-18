package com.chatkeep.admin

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.chatkeep.admin.di.DesktopAppGraph

fun main() = application {
    val lifecycle = LifecycleRegistry()

    // Create dependency graph
    val graph = DesktopAppGraph()

    // Create root component with injected dependencies
    val rootComponent = graph.createRootComponent(
        componentContext = DefaultComponentContext(lifecycle)
    )

    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "ChatKeep Admin",
        state = windowState
    ) {
        LifecycleController(lifecycle, windowState)

        App(
            rootComponent = rootComponent,
            settingsRepository = graph.settingsRepository
        )
    }
}
