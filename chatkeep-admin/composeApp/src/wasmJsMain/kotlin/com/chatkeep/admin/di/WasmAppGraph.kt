package com.chatkeep.admin.di

import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.RootComponent
import com.chatkeep.admin.DefaultRootComponent
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.core.network.di.NetworkModule
import com.chatkeep.admin.feature.auth.domain.repository.AuthRepository
import com.chatkeep.admin.feature.auth.data.repository.AuthRepositoryImpl
import com.chatkeep.admin.feature.settings.domain.SettingsRepository
import com.chatkeep.admin.core.common.TokenStorage

/**
 * Dependency graph for WASM platform.
 * Manual DI - ready for Metro migration when available.
 */
class WasmAppGraph {

    // Platform-specific
    val tokenStorage: TokenStorage = WasmModule.provideTokenStorage()
    val settingsRepository: SettingsRepository = WasmModule.provideSettingsRepository()

    // Network
    private val httpClient = NetworkModule.provideHttpClient()
    val apiService: AdminApiService = NetworkModule.provideAdminApiService(httpClient, tokenStorage)

    // Common
    val baseUrl: String = CommonModule.provideBaseUrl()

    // Repositories
    val authRepository: AuthRepository = AuthRepositoryImpl(apiService, tokenStorage)

    fun createRootComponent(
        componentContext: ComponentContext
    ): RootComponent = DefaultRootComponent(
        componentContext = componentContext,
        authRepository = authRepository,
        apiService = apiService,
        tokenStorage = tokenStorage,
        dataStore = settingsRepository,
        baseUrl = baseUrl
    )
}
