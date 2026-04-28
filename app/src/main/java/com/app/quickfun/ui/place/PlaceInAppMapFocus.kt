package com.app.quickfun.ui.place

import com.app.quickfun.domain.model.Place

/** Есть ли данные, чтобы заведение могло появиться на встроенной карте (координаты или строка для геокодинга). */
fun canFocusPlaceOnInAppMap(place: Place): Boolean {
    val lat = place.latitude
    val lon = place.longitude
    if (lat != null && lon != null && !(lat == 0.0 && lon == 0.0)) return true
    if (!place.city.isNullOrBlank() || !place.address.isNullOrBlank()) return true
    return false
}
