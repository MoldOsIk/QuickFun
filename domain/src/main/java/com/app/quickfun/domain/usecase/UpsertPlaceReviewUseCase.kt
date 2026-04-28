package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.PlaceRepository

class UpsertPlaceReviewUseCase(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(placeId: String, rating: Int, body: String) =
        repository.upsertPlaceReview(placeId, rating, body)
}
