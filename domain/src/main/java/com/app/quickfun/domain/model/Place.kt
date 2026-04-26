package com.app.quickfun.domain.model

data class Place(
    val id: String,
    val name: String,
    val description: String?,
    val status: String?,
    val ownerId: String? = null,
    val categoryName: String?,
    val city: String?,
    val address: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationId: Int? = null,
    val categoryId: Int? = null,
    val coverImageUrl: String? = null,
    val galleryPhotos: List<PlacePhoto> = emptyList()
)