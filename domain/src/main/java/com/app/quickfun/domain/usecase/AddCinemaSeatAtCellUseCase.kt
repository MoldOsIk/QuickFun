package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.BookingRepository

class AddCinemaSeatAtCellUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(
        placeId: String,
        rowNumber: Int,
        seatNumber: Int,
        layoutX: Int,
        layoutY: Int
    ) {
        require(rowNumber >= 1 && seatNumber >= 1) { "Ряд и номер места — не меньше 1." }
        require(layoutX >= 0 && layoutY >= 0) { "Позиция на схеме не может быть отрицательной." }
        val seats = bookingRepository.listSeats(placeId)
        require(seats.none { it.rowNumber == rowNumber && it.seatNumber == seatNumber }) {
            "Место с таким рядом и номером уже есть."
        }
        val label = "$rowNumber.$seatNumber"
        bookingRepository.insertSeat(
            placeId = placeId,
            rowNumber = rowNumber,
            seatNumber = seatNumber,
            label = label,
            layoutX = layoutX,
            layoutY = layoutY
        )
    }
}
