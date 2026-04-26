package com.app.quickfun.map

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun addressLine(city: String?, address: String?): String =
    listOfNotNull(
        address?.trim()?.takeIf { it.isNotEmpty() },
        city?.trim()?.takeIf { it.isNotEmpty() }
    ).joinToString(", ").trim()

/**
 * Цепочка для метки на карте: Яндекс → Android Geocoder → Nominatim (OSM).
 * [placeName] — последний шаг «Название, Беларусь», если по адресу не нашли или адреса нет.
 */
suspend fun resolveMapPinCoordinates(
    context: Context,
    yandexApiKey: String,
    city: String?,
    address: String?,
    placeName: String? = null
): Pair<Double, Double>? {
    if (yandexApiKey.isNotBlank()) {
        YandexGeocoder.forwardGeocodeWithRegionRetries(yandexApiKey, city, address)?.let { return it }
    }
    val android = withContext(Dispatchers.IO) {
        AndroidForwardGeocoder.geocodeBlocking(context, city, address)
    }
    if (android != null) return android

    return withContext(Dispatchers.IO) {
        val line = addressLine(city, address)
        if (line.isNotEmpty()) {
            NominatimGeocoder.geocodeBlocking(line)?.let { return@withContext it }
        }
        val name = placeName?.trim()?.takeIf { it.isNotEmpty() }
        if (name != null) {
            NominatimGeocoder.geocodeBlocking("$name, Беларусь")?.let { return@withContext it }
        }
        null
    }
}
