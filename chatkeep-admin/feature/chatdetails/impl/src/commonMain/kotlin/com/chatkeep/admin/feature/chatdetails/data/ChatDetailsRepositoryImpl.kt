package com.chatkeep.admin.feature.chatdetails.data

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.network.AdminApiService
import com.chatkeep.admin.feature.chatdetails.GatedFeature
import com.chatkeep.admin.feature.chatdetails.domain.ChatDetailsRepository

class ChatDetailsRepositoryImpl(
    private val apiService: AdminApiService
) : ChatDetailsRepository {

    override suspend fun getChatFeatures(chatId: Long): AppResult<List<GatedFeature>> {
        return try {
            val dtos = apiService.getChatFeatures(chatId)
            val features = dtos.map { dto ->
                GatedFeature(
                    key = dto.key,
                    enabled = dto.enabled,
                    name = dto.name,
                    description = dto.description
                )
            }
            AppResult.Success(features)
        } catch (e: Exception) {
            AppResult.Error("Failed to load features: ${e.message}", e)
        }
    }

    override suspend fun setChatFeature(
        chatId: Long,
        featureKey: String,
        enabled: Boolean
    ): AppResult<GatedFeature> {
        return try {
            val dto = apiService.setChatFeature(chatId, featureKey, enabled)
            val feature = GatedFeature(
                key = dto.key,
                enabled = dto.enabled,
                name = dto.name,
                description = dto.description
            )
            AppResult.Success(feature)
        } catch (e: Exception) {
            AppResult.Error("Failed to update feature: ${e.message}", e)
        }
    }
}
