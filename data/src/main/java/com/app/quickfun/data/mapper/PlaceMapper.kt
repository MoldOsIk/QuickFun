package com.app.quickfun.data.mapper

import com.app.quickfun.data.remote.dto.PlaceDto
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.PlacePhoto

fun PlaceDto.toDomain(): Place {
    val gallery = (place_gallery ?: emptyList())
        .sortedBy { it.sort_order }
        .map { PlacePhoto(id = it.id, url = it.image_url) }
    return Place(
        id = id,
        name = name,
        description = description,
        status = status,
        ownerId = owner_id,
        categoryName = categories?.name,
        city = locations?.city,
        address = locations?.address,
        latitude = locations?.latitude,
        longitude = locations?.longitude,
        locationId = location_id,
        categoryId = category_id,
        coverImageUrl = cover_image_url,
        galleryPhotos = gallery
    )
}