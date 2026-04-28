package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.PlaceRepository

class ResubmitRejectedPlaceUseCase(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(placeId: String) = repository.resubmitRejectedPlace(placeId)
}
