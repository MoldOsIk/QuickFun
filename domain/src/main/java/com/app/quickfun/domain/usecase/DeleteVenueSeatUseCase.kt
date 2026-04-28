package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.BookingRepository

class DeleteVenueSeatUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(seatId: Int) {
        bookingRepository.ownerDeleteSeat(seatId)
    }
}
