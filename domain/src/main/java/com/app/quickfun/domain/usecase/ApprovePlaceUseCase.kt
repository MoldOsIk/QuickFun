package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.PlaceRepository

class ApprovePlaceUseCase(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(placeId: String, approved: Boolean, rejectionReason: String? = null) =
        repository.approvePlace(placeId, approved, rejectionReason)
}
