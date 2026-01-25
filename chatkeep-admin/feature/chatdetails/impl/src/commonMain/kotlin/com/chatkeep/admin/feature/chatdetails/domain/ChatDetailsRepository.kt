package com.chatkeep.admin.feature.chatdetails.domain

import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.feature.chatdetails.GatedFeature

interface ChatDetailsRepository {
    suspend fun getChatFeatures(chatId: Long): AppResult<List<GatedFeature>>
    suspend fun setChatFeature(chatId: Long, featureKey: String, enabled: Boolean): AppResult<GatedFeature>
}
