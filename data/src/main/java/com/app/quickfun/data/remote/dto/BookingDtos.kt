package com.app.quickfun.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AvailableBookingSlotsRpcParams(
    val p_place_id: String
)

@Serializable
data class AvailableBookingSlotRowDto(
    val time_slot_id: Int,
    val seat_id: Int,
    val start_time: String,
    val end_time: String,
    val row_number: Int = 0,
    val seat_number: Int = 0,
    val seat_label: String? = null,
    val session_label: String? = null
)

@Serializable
data class BookingInsertDto(
    val user_id: String,
    val time_slot_id: Int,
    val seat_id: Int,
    val status: String = "active"
)

@Serializable
data class TimeSlotInsertDto(
    val place_id: String,
    val start_time: String,
    val end_time: String,
    val label: String? = null
)

@Serializable
data class TimeSlotRowDto(
    val id: Int,
    val place_id: String,
    val start_time: String,
    val end_time: String,
    val label: String? = null
)

@Serializable
data class TimeSlotIdRowDto(
    val id: Int
)

@Serializable
data class BookingTimeSlotJoinDto(
    val start_time: String,
    val end_time: String
)

@Serializable
data class BookingSeatJoinDto(
    val row_number: Int,
    val seat_number: Int,
    val label: String? = null
)

@Serializable
data class UserIdNameRowDto(
    val id: String,
    val name: String? = null,
    val phone_e164: String? = null
)

@Serializable
data class VenueBookingRowDto(
    val id: Int,
    val user_id: String,
    val status: String,
    val time_slots: BookingTimeSlotJoinDto,
    val seats: BookingSeatJoinDto
)

@Serializable
data class SeatRowDto(
    val id: Int,
    val place_id: String,
    val row_number: Int,
    val seat_number: Int,
    val label: String? = null,
    val layout_x: Int? = null,
    val layout_y: Int? = null
)

@Serializable
data class SeatInsertDto(
    val place_id: String,
    val row_number: Int,
    val seat_number: Int,
    val label: String? = null,
    val layout_x: Int? = null,
    val layout_y: Int? = null
)

@Serializable
data class SeatLayoutPatchDto(
    val layout_x: Int,
    val layout_y: Int
)

@Serializable
data class TimeSlotUpdateDto(
    val start_time: String,
    val end_time: String,
    val label: String? = null
)

@Serializable
data class OwnerDeleteSeatRpcParams(val p_seat_id: Int)

@Serializable
data class ClearPlaceSeatsRpcParams(val p_place_id: String)

@Serializable
data class OwnerDeleteTimeSlotRpcParams(val p_slot_id: Int)

@Serializable
data class CancelBookingRpcParams(
    val p_booking_id: Int
)

@Serializable
data class PlaceNameJoinDto(
    val name: String? = null
)

@Serializable
data class MyBookingTimeSlotJoinDto(
    val place_id: String,
    val start_time: String,
    val end_time: String,
    val places: PlaceNameJoinDto? = null
)

@Serializable
data class MyActiveBookingRowDto(
    val id: Int,
    val status: String,
    val time_slots: MyBookingTimeSlotJoinDto,
    val seats: BookingSeatJoinDto
)
