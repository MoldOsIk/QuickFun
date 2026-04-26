package com.app.quickfun.ui.profile.model

data class ProfileState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isRegisteringVenue: Boolean = false,
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val roles: List<String> = emptyList()
)
