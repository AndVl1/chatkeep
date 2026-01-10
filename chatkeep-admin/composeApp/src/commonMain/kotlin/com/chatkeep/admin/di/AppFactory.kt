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
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import com.chatkeep.admin.feature.settings.createSettingsRepository
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

    // AuthRepository is needed by RootComponent to observe auth state and handle logout
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, tokenStorage)
    }

    // SettingsRepository is needed by App to observe theme changes
    val settingsRepository: SettingsRepository by lazy {
        createSettingsRepository(dataStore)
    }

    /**
     * Creates the root component with all dependencies wired up.
     */
    fun createRootComponent(componentContext: ComponentContext): RootComponent {
        return DefaultRootComponent(
            componentContext = componentContext,
            authRepository = authRepository,
            apiService = apiService,
            tokenStorage = tokenStorage,
            dataStore = dataStore
        )
    }
}
