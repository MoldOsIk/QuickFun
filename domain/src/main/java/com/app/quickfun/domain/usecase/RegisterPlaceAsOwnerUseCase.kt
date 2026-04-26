package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.PlaceRegistrationDraft
import com.app.quickfun.domain.model.coordinatesOrAddressMessage
import com.app.quickfun.domain.repository.PlaceRepository

class RegisterPlaceAsOwnerUseCase(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(draft: PlaceRegistrationDraft) {
        draft.coordinatesOrAddressMessage()?.let { throw IllegalArgumentException(it) }
        repository.registerPlaceAsOwner(draft)
    }
}
