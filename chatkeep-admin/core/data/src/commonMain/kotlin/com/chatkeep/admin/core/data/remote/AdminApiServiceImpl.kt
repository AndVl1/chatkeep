package com.chatkeep.admin.core.data.remote

import com.chatkeep.admin.core.data.local.TokenStorage
import com.chatkeep.admin.core.data.remote.dto.ActionResponse
import com.chatkeep.admin.core.data.remote.dto.AdminResponse
import com.chatkeep.admin.core.data.remote.dto.ChatResponse
import com.chatkeep.admin.core.data.remote.dto.DashboardResponse
import com.chatkeep.admin.core.data.remote.dto.LoginRequest
import com.chatkeep.admin.core.data.remote.dto.LoginResponse
import com.chatkeep.admin.core.data.remote.dto.LogsResponse
import com.chatkeep.admin.core.data.remote.dto.TriggerResponse
import com.chatkeep.admin.core.data.remote.dto.WorkflowResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class AdminApiServiceImpl(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) : AdminApiService {

    override suspend fun login(request: LoginRequest): LoginResponse {
        return httpClient.post("/api/v1/admin/auth/login") {
            contentType(ContentType.Application.Json)
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

    override suspend fun getLogs(lines: Int): LogsResponse {
        return httpClient.get("/api/v1/admin/logs") {
            addAuthHeader()
            parameter("lines", lines)
        }.body()
    }

    override suspend fun restartBot(): ActionResponse {
        return httpClient.post("/api/v1/admin/actions/restart") {
            addAuthHeader()
        }.body()
    }

    private suspend fun io.ktor.client.request.HttpRequestBuilder.addAuthHeader() {
        val token = tokenStorage.getToken()
        if (token != null) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}
