package com.app.quickfun.ui.auth.model

import com.app.quickfun.domain.model.PlaceRegistrationDraft

sealed interface RegistrationMode {
    data object User : RegistrationMode
    data class PlaceOwner(val draft: PlaceRegistrationDraft) : RegistrationMode
}

sealed interface AuthIntent {
    data object CheckSession : AuthIntent
    data class SignIn(val email: String, val password: String) : AuthIntent
    data class SignUp(
        val email: String,
        val password: String,
        val displayName: String,
        val mode: RegistrationMode = RegistrationMode.User,
        val phoneE164: String? = null
    ) : AuthIntent
    data object DismissEmailConfirmation : AuthIntent
    data object BackToUnauthorized : AuthIntent
    data object SignOut : AuthIntent
}
