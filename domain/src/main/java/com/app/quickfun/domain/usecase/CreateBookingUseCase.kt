package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.BookingRepository

class CreateBookingUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(timeSlotId: Int, seatId: Int) {
        bookingRepository.createBooking(timeSlotId, seatId)
    }
}
