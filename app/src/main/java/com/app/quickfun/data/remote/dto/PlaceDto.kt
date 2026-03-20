package com.app.quickfun.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaceDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val status: String? = null,
    val categories: CategoryDto? = null,
    val locations: LocationDto? = null
)

@Serializable
data class CategoryDto(
    val name: String
)

@Serializable
data class LocationDto(
    val city: String? = null,
    val address: String? = null
)