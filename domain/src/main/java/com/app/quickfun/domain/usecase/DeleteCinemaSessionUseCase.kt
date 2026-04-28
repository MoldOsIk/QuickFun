package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.BookingRepository

class DeleteCinemaSessionUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(timeSlotId: Int) {
        bookingRepository.ownerDeleteTimeSlot(timeSlotId)
    }
}
