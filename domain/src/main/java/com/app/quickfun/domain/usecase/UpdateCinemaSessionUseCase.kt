package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.BookingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class UpdateCinemaSessionUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(
        placeId: String,
        timeSlotId: Int,
        filmTitle: String,
        dateYmd: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        val title = filmTitle.trim()
        require(title.isNotEmpty()) { "Укажите название фильма." }
        val zone = ZoneId.systemDefault()
        val date = LocalDate.parse(dateYmd.trim())
        val start = LocalDateTime.of(date, LocalTime.of(startHour, startMinute))
        val end = LocalDateTime.of(date, LocalTime.of(endHour, endMinute))
        require(end.isAfter(start)) { "Конец сеанса должен быть позже начала." }
        val utcStart = start.atZone(zone).withZoneSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val utcEnd = end.atZone(zone).withZoneSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        bookingRepository.updateTimeSlot(timeSlotId, placeId, utcStart, utcEnd, title)
    }
}
