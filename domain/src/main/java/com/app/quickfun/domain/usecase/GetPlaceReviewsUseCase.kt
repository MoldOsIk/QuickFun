package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.PlaceReview
import com.app.quickfun.domain.repository.PlaceRepository

class GetPlaceReviewsUseCase(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(placeId: String): List<PlaceReview> =
        repository.getPlaceReviews(placeId)
}
