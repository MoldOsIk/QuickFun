package com.app.quickfun.ui.place

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
