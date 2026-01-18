package com.chatkeep.admin

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.chatkeep.admin.di.IosAppGraph
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    // Create dependency graph
    val graph = IosAppGraph()

    // Create root component with injected dependencies
    val rootComponent = graph.createRootComponent(
        componentContext = DefaultComponentContext(
            lifecycle = ApplicationLifecycle()
        )
    )

    return ComposeUIViewController {
        App(
            rootComponent = rootComponent,
            settingsRepository = graph.settingsRepository
        )
    }
}
