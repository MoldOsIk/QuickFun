package com.app.quickfun.data.mapper

import com.app.quickfun.data.remote.dto.PlaceDto
import com.app.quickfun.data.remote.dto.PlaceReviewRowDto
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.PlacePhoto
import com.app.quickfun.domain.model.PlaceReview

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
        galleryPhotos = gallery,
        rejectionReason = rejection_reason?.trim()?.takeIf { it.isNotEmpty() }
    )
}

fun PlaceReviewRowDto.toDomain(): PlaceReview =
    PlaceReview(
        id = id,
        placeId = place_id,
        userId = user_id,
        authorDisplayName = users?.name?.trim()?.takeIf { it.isNotEmpty() },
        rating = rating,
        body = body,
        createdAtIso = created_at,
        updatedAtIso = updated_at
    )