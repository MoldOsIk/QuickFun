package com.app.quickfun.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationInsertDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val city: String? = null
)

@Serializable
data class LocationIdRow(
    val id: Int
)

@Serializable
data class PlaceInsertDto(
    val name: String,
    val description: String? = null,
    val category_id: Int? = null,
    val location_id: Int,
    val owner_id: String,
    val status: String
)

@Serializable
data class LocationUpdateDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val city: String? = null
)

@Serializable
data class PlaceUpdateDto(
    val name: String,
    val description: String? = null,
    val category_id: Int? = null,
    val status: String,
    val cover_image_url: String? = null
)

@Serializable
data class PlaceGalleryInsertDto(
    val place_id: String,
    val image_url: String,
    val sort_order: Int
)

@Serializable
data class IdLongRow(
    val id: Long
)

@Serializable
data class PlaceCoverOnlyUpdateDto(
    val cover_image_url: String?
)
