package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.Seat
import com.app.quickfun.domain.repository.BookingRepository

class ListSeatsUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(placeId: String): List<Seat> =
        bookingRepository.listSeats(placeId)
}
