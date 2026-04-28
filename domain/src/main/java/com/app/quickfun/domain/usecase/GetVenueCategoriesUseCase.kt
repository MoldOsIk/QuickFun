package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.VenueCategory
import com.app.quickfun.domain.repository.PlaceRepository

class GetVenueCategoriesUseCase(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(): List<VenueCategory> = repository.getVenueCategories()
}
