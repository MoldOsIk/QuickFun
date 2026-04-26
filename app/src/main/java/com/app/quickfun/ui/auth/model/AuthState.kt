package com.app.quickfun.ui.auth.model

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthorized : AuthState()
    data object Authorized : AuthState()
    data class AwaitingEmailConfirmation(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
