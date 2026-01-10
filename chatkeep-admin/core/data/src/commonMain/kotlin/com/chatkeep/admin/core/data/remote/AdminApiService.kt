package com.chatkeep.admin.core.data.remote

import com.chatkeep.admin.core.data.remote.dto.ActionResponse
import com.chatkeep.admin.core.data.remote.dto.AdminResponse
import com.chatkeep.admin.core.data.remote.dto.ChatResponse
import com.chatkeep.admin.core.data.remote.dto.DashboardResponse
import com.chatkeep.admin.core.data.remote.dto.LoginRequest
import com.chatkeep.admin.core.data.remote.dto.LoginResponse
import com.chatkeep.admin.core.data.remote.dto.LogsResponse
import com.chatkeep.admin.core.data.remote.dto.TriggerResponse
import com.chatkeep.admin.core.data.remote.dto.WorkflowResponse

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
