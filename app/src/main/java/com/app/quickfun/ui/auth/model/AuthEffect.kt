package com.app.quickfun.ui.auth.model

sealed interface AuthEffect {
    data class ShowMessage(val message: String) : AuthEffect
}
