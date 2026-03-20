package com.app.quickfun.data.mapper

import com.app.quickfun.data.remote.dto.PlaceDto
import com.app.quickfun.domain.model.Place

fun PlaceDto.toDomain(): Place {
    return Place(
        id = id,
        name = name,
        description = description,
        status = status,
        categoryName = categories?.name,
        city = locations?.city,
        address = locations?.address
    )
}