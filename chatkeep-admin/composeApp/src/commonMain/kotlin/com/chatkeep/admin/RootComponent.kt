package com.chatkeep.admin

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.chatkeep.admin.core.common.DeepLinkData
import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.auth.AuthComponent
import com.chatkeep.admin.feature.auth.AuthState
import com.chatkeep.admin.feature.auth.createAuthComponent
import com.chatkeep.admin.feature.auth.domain.repository.AuthRepository
import com.chatkeep.admin.feature.main.MainComponent
import com.chatkeep.admin.feature.main.createMainComponent
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>
    fun handleDeepLink(data: DeepLinkData)

    sealed class Child {
        data class Auth(val component: AuthComponent) : Child()
        data class Main(val component: MainComponent) : Child()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val apiService: AdminApiService,
    private val tokenStorage: TokenStorage,
    private val dataStore: DataStore<Preferences>,
    private val baseUrl: String
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
        return createAuthComponent(
            componentContext = context,
            apiService = apiService,
            tokenStorage = tokenStorage,
            baseUrl = baseUrl,
            onSuccess = { /* Auth state change will trigger navigation */ }
        )
    }

    private fun createMainComponent(context: ComponentContext): MainComponent {
        return createMainComponent(
            componentContext = context,
            apiService = apiService,
            dataStore = dataStore,
            onLogout = {
                scope.launch {
                    authRepository.logout()
                }
            }
        )
    }

    override fun handleDeepLink(data: DeepLinkData) {
        // Get the current active child
        val currentChild = childStack.value.active.instance

        // If we're on the Auth screen, forward the deeplink to the AuthComponent
        if (currentChild is RootComponent.Child.Auth) {
            currentChild.component.onDeepLinkReceived(data)
        }
    }
}
