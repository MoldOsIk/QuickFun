package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.PlaceRepository

class GetPendingPlacesUseCase(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke() = repository.getPendingPlacesForModeration()
}
