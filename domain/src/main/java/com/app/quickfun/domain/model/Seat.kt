package com.app.quickfun.domain.model

data class Seat(
    val id: Int,
    val placeId: String,
    val rowNumber: Int,
    val seatNumber: Int,
    val label: String? = null,
    val layoutX: Int? = null,
    val layoutY: Int? = null
) {
    fun displayLabel(): String =
        label?.trim()?.takeIf { it.isNotEmpty() } ?: "Ряд $rowNumber, место $seatNumber"

    /** Короткая подпись на схеме зала: «ряд.место». */
    fun rowDotSeatLabel(): String = "$rowNumber.$seatNumber"
}
