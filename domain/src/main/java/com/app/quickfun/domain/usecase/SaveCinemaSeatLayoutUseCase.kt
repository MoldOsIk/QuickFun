package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.SeatLayoutPosition
import com.app.quickfun.domain.repository.BookingRepository

class SaveCinemaSeatLayoutUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(placeId: String, positions: List<SeatLayoutPosition>) {
        require(positions.isNotEmpty()) { "Нет позиций для сохранения." }
        positions.forEach { p ->
            require(p.layoutX >= 0 && p.layoutY >= 0) {
                "Координаты схемы не могут быть отрицательными."
            }
        }
        val cells = positions.map { it.layoutX to it.layoutY }.toSet()
        require(cells.size == positions.size) {
            "Два места не могут стоять в одной клетке схемы."
        }
        bookingRepository.updateSeatLayouts(placeId, positions)
    }
}
