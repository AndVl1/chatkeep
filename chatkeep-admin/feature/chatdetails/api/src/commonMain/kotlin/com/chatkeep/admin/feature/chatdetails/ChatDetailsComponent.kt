package com.chatkeep.admin.feature.chatdetails

import com.arkivanov.decompose.value.Value

interface ChatDetailsComponent {
    val state: Value<ChatDetailsState>
    
    fun onFeatureToggle(featureKey: String, enabled: Boolean)
    fun onBackClick()
}
