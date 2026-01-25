package com.chatkeep.admin.feature.chatdetails.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.chatdetails.GatedFeature

class SetChatFeatureUseCase(
    private val repository: ChatDetailsRepository
) {
    suspend fun execute(chatId: Long, featureKey: String, enabled: Boolean): AppResult<GatedFeature> {
        return repository.setChatFeature(chatId, featureKey, enabled)
    }
}
