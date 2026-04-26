package com.app.quickfun.domain.model

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val roles: List<String> = emptyList()
)
