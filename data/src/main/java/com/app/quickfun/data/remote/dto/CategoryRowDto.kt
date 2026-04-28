package com.app.quickfun.data.remote.dto

import com.app.quickfun.domain.model.VenueCategory
import kotlinx.serialization.Serializable

@Serializable
data class CategoryRowDto(
    val id: Int,
    val name: String
)

fun CategoryRowDto.toVenueCategory(): VenueCategory = VenueCategory(id = id, name = name.trim())
