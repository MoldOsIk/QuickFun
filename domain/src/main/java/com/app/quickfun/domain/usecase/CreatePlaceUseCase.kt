package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.coordinatesOrAddressError
import com.app.quickfun.domain.repository.PlaceRepository

class CreatePlaceUseCase(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(
        name: String,
        description: String?,
        city: String?,
        address: String?,
        categoryId: Int?,
        latitude: Double?,
        longitude: Double?
    ) {
        coordinatesOrAddressError(latitude, longitude, city, address)?.let {
            throw IllegalArgumentException(it)
        }
        repository.createPlace(
            name = name,
            description = description,
            city = city,
            address = address,
            categoryId = categoryId,
            latitude = latitude,
            longitude = longitude
        )
    }
}
