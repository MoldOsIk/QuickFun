package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.BookingRepository

class AddSeatUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(
        placeId: String,
        rowNumber: Int,
        seatNumber: Int,
        label: String? = null
    ) {
        bookingRepository.insertSeat(placeId, rowNumber, seatNumber, label)
    }
}
