package com.app.quickfun.domain.model

data class VenueBooking(
    val id: Int,
    val userId: String,
    val guestName: String?,
    val startTimeIso: String,
    val endTimeIso: String,
    val rowNumber: Int,
    val seatNumber: Int,
    val seatLabel: String? = null,
    val status: String
) {
    fun displaySeat(): String =
        seatLabel?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Ряд $rowNumber, место $seatNumber"
}
