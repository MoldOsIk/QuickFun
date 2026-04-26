package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.TimeSlotToInsert
import com.app.quickfun.domain.repository.BookingRepository

class AddCinemaSessionUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(placeId: String, filmTitle: String, startUtcIso: String, endUtcIso: String) {
        val title = filmTitle.trim()
        require(title.isNotEmpty()) { "Укажите название сеанса / фильма." }
        require(startUtcIso.isNotBlank() && endUtcIso.isNotBlank()) { "Укажите начало и конец сеанса." }
        bookingRepository.insertTimeSlots(
            placeId,
            listOf(
                TimeSlotToInsert(
                    startTimeUtcIso = startUtcIso.trim(),
                    endTimeUtcIso = endUtcIso.trim(),
                    sessionLabel = title
                )
            )
        )
    }
}
