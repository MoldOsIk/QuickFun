package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.BookingRepository

class GetMyActiveBookingsUseCase(
    private val repository: BookingRepository
) {
    suspend operator fun invoke() = repository.listMyActiveBookings()
}
