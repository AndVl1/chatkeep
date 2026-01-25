package com.chatkeep.admin.feature.chatdetails

sealed class ChatDetailsState {
    data object Loading : ChatDetailsState()
    
    data class Success(
        val chatId: Long,
        val chatTitle: String,
        val features: List<GatedFeature>
    ) : ChatDetailsState()
    
    data class Error(val message: String) : ChatDetailsState()
}

data class GatedFeature(
    val key: String,
    val enabled: Boolean,
    val name: String,
    val description: String
)
