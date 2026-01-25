package com.chatkeep.admin.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets

class AdminApiServiceImpl(
    private val httpClient: HttpClient,
    private val tokenProvider: () -> String?
) : AdminApiService {

    override suspend fun login(request: LoginRequest): LoginResponse {
        return httpClient.post("/api/v1/admin/auth/login") {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            setBody(request)
        }.body()
    }

    override suspend fun getMe(): AdminResponse {
        return httpClient.get("/api/v1/admin/auth/me") {
            addAuthHeader()
        }.body()
    }

    override suspend fun getDashboard(): DashboardResponse {
        return httpClient.get("/api/v1/admin/dashboard") {
            addAuthHeader()
        }.body()
    }

    override suspend fun getChats(): List<ChatResponse> {
        return httpClient.get("/api/v1/admin/chats") {
            addAuthHeader()
        }.body()
    }

    override suspend fun getWorkflows(): List<WorkflowResponse> {
        return httpClient.get("/api/v1/admin/workflows") {
            addAuthHeader()
        }.body()
    }

    override suspend fun triggerWorkflow(workflowId: String): TriggerResponse {
        return httpClient.post("/api/v1/admin/workflows/$workflowId/trigger") {
            addAuthHeader()
        }.body()
    }

    override suspend fun getLogs(
        minutes: Int,
        level: String,
        filter: String?
    ): LogsResponse {
        return httpClient.get("/api/v1/admin/logs") {
            addAuthHeader()
            parameter("minutes", minutes)
            parameter("level", level)
            if (filter != null) {
                parameter("filter", filter)
            }
        }.body()
    }

    override suspend fun restartBot(): ActionResponse {
        return httpClient.post("/api/v1/admin/actions/restart") {
            addAuthHeader()
        }.body()
    }

    override suspend fun getChatFeatures(chatId: Long): List<GatedFeatureDto> {
        return httpClient.get("/api/v1/admin/chats/$chatId/features") {
            addAuthHeader()
        }.body()
    }

    override suspend fun setChatFeature(chatId: Long, featureKey: String, enabled: Boolean): GatedFeatureDto {
        return httpClient.put("/api/v1/admin/chats/$chatId/features/$featureKey") {
            addAuthHeader()
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            setBody(SetFeatureRequest(enabled))
        }.body()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.addAuthHeader() {
        val token = tokenProvider()
        if (token != null) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}
