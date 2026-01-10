package com.chatkeep.admin.feature.auth

import com.arkivanov.decompose.value.Value

interface AuthComponent {
    val state: Value<AuthState>
    fun onLoginClick()
    fun onRetry()

    sealed class AuthState {
        data object Idle : AuthState()
        data object Loading : AuthState()
        data class Error(val message: String) : AuthState()
        data object Success : AuthState()
    }
}
