package com.chatkeep.admin.core.data.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.data.remote.AdminApiService
import com.chatkeep.admin.core.domain.repository.ActionResult
import com.chatkeep.admin.core.domain.repository.ActionsRepository

class ActionsRepositoryImpl(
    private val apiService: AdminApiService
) : ActionsRepository {

    override suspend fun restartBot(): AppResult<ActionResult> {
        return try {
            val response = apiService.restartBot()
            AppResult.Success(
                ActionResult(
                    success = response.success,
                    message = response.message
                )
            )
        } catch (e: Exception) {
            AppResult.Error("Failed to restart bot: ${e.message}", e)
        }
    }
}
