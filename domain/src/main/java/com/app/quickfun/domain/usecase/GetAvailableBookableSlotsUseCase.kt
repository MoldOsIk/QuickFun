package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.BookableSlot
import com.app.quickfun.domain.repository.BookingRepository

class GetAvailableBookableSlotsUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(placeId: String): List<BookableSlot> =
        bookingRepository.getAvailableSlots(placeId)
}
