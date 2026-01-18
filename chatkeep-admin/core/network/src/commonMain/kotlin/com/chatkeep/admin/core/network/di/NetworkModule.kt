package com.chatkeep.admin.core.network.di

import com.chatkeep.admin.core.common.TokenStorage
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.core.network.AdminApiServiceImpl
import com.chatkeep.admin.core.network.createTokenProvider
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Provides network-related dependencies.
 * Manual DI module - ready for Metro migration when available.
 */
object NetworkModule {

    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun provideHttpClient(json: Json = provideJson()): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    fun provideAdminApiService(
        httpClient: HttpClient,
        tokenStorage: TokenStorage
    ): AdminApiService = AdminApiServiceImpl(
        httpClient = httpClient,
        tokenProvider = createTokenProvider(tokenStorage)
    )
}
