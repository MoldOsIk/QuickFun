package com.app.quickfun.ui.place

import com.app.quickfun.domain.model.coordinatesOrAddressError

fun parseCoord(raw: String): Double? {
    val t = raw.trim().replace(',', '.').replace(" ", "")
    if (t.isEmpty()) return null
    return t.toDoubleOrNull()
}

fun formatCoord(value: Double): String = String.format("%.6f", value)

data class ParsedLatLng(val latitude: Double?, val longitude: Double?, val parseError: String?)

/** Обе строки пустые → null,null; обе заполнены и парсятся → числа; иначе сообщение об ошибке. */
fun parseOptionalLatLonStrings(latRaw: String, lngRaw: String): ParsedLatLng {
    val latTrim = latRaw.trim()
    val lngTrim = lngRaw.trim()
    return when {
        latTrim.isEmpty() && lngTrim.isEmpty() -> ParsedLatLng(null, null, null)
        latTrim.isEmpty() || lngTrim.isEmpty() -> ParsedLatLng(
            null,
            null,
            "Укажите обе координаты или оставьте оба поля пустыми."
        )
        else -> {
            val lat = parseCoord(latRaw)
            val lng = parseCoord(lngRaw)
            if (lat == null || lng == null) {
                ParsedLatLng(null, null, "Неверный формат широты или долготы.")
            } else {
                ParsedLatLng(lat, lng, null)
            }
        }
    }
}

/** Сообщение для Snackbar или null, если можно отправлять форму. */
fun venueFormCoordsAndAddressError(
    latRaw: String,
    lngRaw: String,
    city: String?,
    address: String?
): String? {
    val p = parseOptionalLatLonStrings(latRaw, lngRaw)
    if (p.parseError != null) return p.parseError
    return coordinatesOrAddressError(p.latitude, p.longitude, city, address)
}
