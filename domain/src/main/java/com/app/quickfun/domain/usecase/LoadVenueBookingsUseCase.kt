package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.VenueBooking
import com.app.quickfun.domain.repository.BookingRepository

class LoadVenueBookingsUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(placeId: String): List<VenueBooking> =
        bookingRepository.listVenueBookings(placeId)
}
