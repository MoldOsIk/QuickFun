package com.app.quickfun.ui.place

import com.app.quickfun.domain.model.TimeSlot
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fmt = DateTimeFormatter.ofPattern("EEE dd.MM, HH:mm", Locale.forLanguageTag("ru"))

fun formatBookingSlotRange(startIso: String, endIso: String): String {
    val zone = ZoneId.systemDefault()
    val s = parseUtcToLocal(startIso, zone)
    val e = parseUtcToLocal(endIso, zone)
    return "${s.format(fmt)} — ${e.format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("ru")))}"
}

private fun parseUtcToLocal(iso: String, zone: ZoneId): LocalDateTime {
    val n = iso.trim().replaceFirst(" ", "T").take(19)
    val ldt = LocalDateTime.parse(n)
    return ldt.atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDateTime()
}

/** Дата начала слота в локальной зоне (ГГГГ-ММ-ДД). */
fun timeSlotStartLocalDateYmd(ts: TimeSlot): String {
    val zone = ZoneId.systemDefault()
    return parseUtcToLocal(ts.startTimeIso, zone).toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
}

fun timeSlotStartLocalHourMinute(ts: TimeSlot): Pair<Int, Int> {
    val zone = ZoneId.systemDefault()
    val t = parseUtcToLocal(ts.startTimeIso, zone).toLocalTime()
    return t.hour to t.minute
}

fun timeSlotEndLocalHourMinute(ts: TimeSlot): Pair<Int, Int> {
    val zone = ZoneId.systemDefault()
    val t = parseUtcToLocal(ts.endTimeIso, zone).toLocalTime()
    return t.hour to t.minute
}
