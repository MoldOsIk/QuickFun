package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.TimeSlotToInsert
import com.app.quickfun.domain.repository.BookingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CreateWeekTimeSlotsUseCase(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(
        placeId: String,
        weekdays: Set<Int>,
        startHour: Int,
        endHour: Int,
        slotMinutes: Int
    ) {
        require(weekdays.isNotEmpty()) { "Выберите хотя бы один день недели." }
        require(startHour in 0..22 && endHour in 1..23 && endHour > startHour) {
            "Укажите часы: начало и конец в пределах одного дня (конец позже начала)."
        }
        require(slotMinutes in listOf(15, 30, 45, 60, 90, 120)) {
            "Длительность слота: 15–120 минут (шаг 15)."
        }

        val zone = ZoneId.systemDefault()
        val out = mutableListOf<TimeSlotToInsert>()
        for (offset in 0..7) {
            val date = LocalDate.now(zone).plusDays(offset.toLong())
            val dow = date.dayOfWeek.value
            if (dow !in weekdays) continue
            var cur = LocalDateTime.of(date, LocalTime.of(startHour, 0))
            val dayEnd = LocalDateTime.of(date, LocalTime.of(endHour, 0))
            while (true) {
                val next = cur.plusMinutes(slotMinutes.toLong())
                if (next > dayEnd) break
                val utcStart = cur.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
                val utcEnd = next.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
                out.add(
                    TimeSlotToInsert(
                        startTimeUtcIso = utcStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        endTimeUtcIso = utcEnd.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        sessionLabel = null
                    )
                )
                cur = next
            }
        }
        if (out.isEmpty()) {
            throw IllegalStateException(
                "Не получилось ни одного слота: выберите дни в ближайшие 8 дней и проверьте, что " +
                    "длительность слота не больше интервала «час окончания − час начала» " +
                    "(например, при 10–11 ч подходит до 60 минут)."
            )
        }
        bookingRepository.insertTimeSlots(placeId, out)
    }
}
