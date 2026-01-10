package com.chatkeep.admin.core.data.repository

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.data.mapper.toDomain
import com.chatkeep.admin.core.data.remote.AdminApiService
import com.chatkeep.admin.core.domain.model.Chat
import com.chatkeep.admin.core.domain.repository.ChatsRepository

class ChatsRepositoryImpl(
    private val apiService: AdminApiService
) : ChatsRepository {

    override suspend fun getChats(): AppResult<List<Chat>> {
        return try {
            val response = apiService.getChats()
            AppResult.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            AppResult.Error("Failed to load chats: ${e.message}", e)
        }
    }
}
