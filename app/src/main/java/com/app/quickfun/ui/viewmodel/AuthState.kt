package com.app.quickfun.ui.viewmodel

sealed class AuthState {
    object Loading : AuthState()
    object Unauthorized : AuthState()
    object Authorized : AuthState()
    data class AwaitingEmailConfirmation(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}