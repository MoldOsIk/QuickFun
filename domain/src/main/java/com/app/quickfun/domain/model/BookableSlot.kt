package com.app.quickfun.domain.model

/** Свободный вариант брони (слот + место). */
data class BookableSlot(
    val timeSlotId: Int,
    val seatId: Int,
    val startTimeIso: String,
    val endTimeIso: String,
    val rowNumber: Int = 0,
    val seatNumber: Int = 0,
    val seatLabel: String? = null,
    val sessionLabel: String? = null
) {
    fun displaySeat(): String =
        seatLabel?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Ряд $rowNumber, место $seatNumber"

    fun displaySession(fallbackTimeLine: String): String =
        sessionLabel?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackTimeLine
}
