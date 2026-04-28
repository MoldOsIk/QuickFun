package com.app.quickfun.domain.model

/**
 * Активная бронь текущего пользователя (каталог / профиль).
 */
data class MyActiveBooking(
    val bookingId: Int,
    val placeId: String,
    val placeName: String,
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
