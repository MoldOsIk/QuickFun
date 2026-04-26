package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.Seat
import com.app.quickfun.domain.model.TimeSlot
import com.app.quickfun.domain.repository.BookingRepository

data class OwnerBookingSchedule(
    val seats: List<Seat>,
    val timeSlots: List<TimeSlot>
)

class LoadOwnerBookingScheduleUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(placeId: String): OwnerBookingSchedule {
        val seats = bookingRepository.listSeats(placeId)
        val slots = bookingRepository.listTimeSlotsFromNow(placeId)
        return OwnerBookingSchedule(seats = seats, timeSlots = slots)
    }
}
