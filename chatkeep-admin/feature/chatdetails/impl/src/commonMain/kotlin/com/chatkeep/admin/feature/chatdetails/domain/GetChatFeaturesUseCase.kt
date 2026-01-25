package com.chatkeep.admin.feature.chatdetails.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.chatdetails.GatedFeature

class GetChatFeaturesUseCase(
    private val repository: ChatDetailsRepository
) {
    suspend fun execute(chatId: Long): AppResult<List<GatedFeature>> {
        return repository.getChatFeatures(chatId)
    }
}
