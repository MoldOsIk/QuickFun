package com.app.quickfun.domain.model

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    /** E.164, например +79001234567 */
    val phoneE164: String? = null,
    val roles: List<String> = emptyList()
)
