package com.app.quickfun.domain.model

data class PlaceReview(
    val id: Long,
    val placeId: String,
    val userId: String,
    /** Имя из `public.users.name` (может быть пустым). */
    val authorDisplayName: String?,
    val rating: Int,
    val body: String,
    val createdAtIso: String,
    val updatedAtIso: String
)
