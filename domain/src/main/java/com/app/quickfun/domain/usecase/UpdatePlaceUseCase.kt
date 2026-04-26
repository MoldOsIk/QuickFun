package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.coordinatesOrAddressError
import com.app.quickfun.domain.repository.PlaceRepository

class UpdatePlaceUseCase(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(
        placeId: String,
        locationId: Int,
        name: String,
        description: String?,
        city: String?,
        address: String?,
        categoryId: Int?,
        latitude: Double?,
        longitude: Double?,
        status: String,
        coverImageUrl: String?
    ) {
        coordinatesOrAddressError(latitude, longitude, city, address)?.let {
            throw IllegalArgumentException(it)
        }
        repository.updatePlace(
            placeId = placeId,
            locationId = locationId,
            name = name,
            description = description,
            city = city,
            address = address,
            categoryId = categoryId,
            latitude = latitude,
            longitude = longitude,
            status = status,
            coverImageUrl = coverImageUrl
        )
    }
}
