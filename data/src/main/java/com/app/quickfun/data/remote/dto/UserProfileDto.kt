package com.app.quickfun.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileRowDto(
    val id: String,
    val name: String? = null,
    @SerialName("user_roles")
    val userRoles: List<UserRoleRowDto> = emptyList()
)

@Serializable
data class UserRoleRowDto(
    val roles: RoleRowDto? = null
)

@Serializable
data class RoleRowDto(
    val name: String? = null
)

@Serializable
data class UserUpsertDto(
    val id: String,
    val name: String? = null
)
