package com.app.quickfun.domain.repository

import com.app.quickfun.domain.model.BookableSlot
import com.app.quickfun.domain.model.Seat
import com.app.quickfun.domain.model.SeatLayoutPosition
import com.app.quickfun.domain.model.TimeSlot
import com.app.quickfun.domain.model.TimeSlotToInsert
import com.app.quickfun.domain.model.VenueBooking

interface BookingRepository {
    suspend fun getAvailableSlots(placeId: String): List<BookableSlot>
    suspend fun createBooking(timeSlotId: Int, seatId: Int)
    suspend fun listSeats(placeId: String): List<Seat>
    suspend fun listTimeSlotsFromNow(placeId: String): List<TimeSlot>
    suspend fun insertTimeSlots(placeId: String, slots: List<TimeSlotToInsert>)
    suspend fun insertSeat(placeId: String, rowNumber: Int, seatNumber: Int, label: String? = null)
    suspend fun insertSeatsBatch(placeId: String, seats: List<Triple<Int, Int, String?>>)

    suspend fun updateSeatLayouts(placeId: String, positions: List<SeatLayoutPosition>)
    suspend fun listVenueBookings(placeId: String): List<VenueBooking>
}
