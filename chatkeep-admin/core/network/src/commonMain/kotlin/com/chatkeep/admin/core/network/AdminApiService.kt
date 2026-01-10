package com.chatkeep.admin.core.network

interface AdminApiService {
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun getMe(): AdminResponse
    suspend fun getDashboard(): DashboardResponse
    suspend fun getChats(): List<ChatResponse>
    suspend fun getWorkflows(): List<WorkflowResponse>
    suspend fun triggerWorkflow(workflowId: String): TriggerResponse
    suspend fun getLogs(lines: Int): LogsResponse
    suspend fun restartBot(): ActionResponse
}
