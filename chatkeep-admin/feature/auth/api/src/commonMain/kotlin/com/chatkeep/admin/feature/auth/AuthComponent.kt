package com.chatkeep.admin.feature.auth

import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.DeepLinkData

interface AuthComponent {
    val state: Value<AuthState>
    fun onLoginClick()
    fun onRetry()
    fun onDeepLinkReceived(data: DeepLinkData)

    sealed class AuthState {
        data object Idle : AuthState()
        data object Loading : AuthState()
        data class Error(val message: String) : AuthState()
        data object Success : AuthState()
    }
}
