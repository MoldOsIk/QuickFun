package com.app.quickfun.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaceGalleryRowDto(
    val id: Long,
    val image_url: String,
    val sort_order: Int = 0
)

@Serializable
data class PlaceDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val status: String? = null,
    val owner_id: String? = null,
    val location_id: Int? = null,
    val category_id: Int? = null,
    val rejection_reason: String? = null,
    val cover_image_url: String? = null,
    val place_gallery: List<PlaceGalleryRowDto>? = null,
    val categories: CategoryDto? = null,
    val locations: LocationDto? = null
)

@Serializable
data class CategoryDto(
    val name: String
)

@Serializable
data class LocationDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String? = null,
    val address: String? = null
)