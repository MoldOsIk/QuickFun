package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.BookingRepository

class ClearCinemaHallUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(placeId: String) {
        bookingRepository.clearPlaceSeats(placeId)
    }
}
