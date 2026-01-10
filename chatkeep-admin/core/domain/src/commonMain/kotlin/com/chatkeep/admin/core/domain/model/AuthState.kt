package com.chatkeep.admin.core.domain.model

sealed class AuthState {
    data object Loading : AuthState()
    data object NotAuthenticated : AuthState()
    data class Authenticated(val admin: Admin, val token: String) : AuthState()
}
