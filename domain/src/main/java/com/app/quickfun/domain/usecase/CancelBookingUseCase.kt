package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.BookingRepository

class CancelBookingUseCase(
    private val repository: BookingRepository
) {
    suspend operator fun invoke(bookingId: Int) = repository.cancelBooking(bookingId)
}
