package com.chatkeep.admin.feature.dashboard.data.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.dashboard.ActionResult
import com.chatkeep.admin.feature.dashboard.domain.repository.ActionsRepository

internal class ActionsRepositoryImpl(
    private val apiService: AdminApiService
) : ActionsRepository {

    override suspend fun restartBot(): AppResult<ActionResult> {
        return try {
            val response = apiService.restartBot()
            val result = ActionResult(
                success = response.success,
                message = response.message
            )
            AppResult.Success(result)
        } catch (e: Exception) {
            AppResult.Error("Failed to restart bot: ${e.message}", e)
        }
    }
}
