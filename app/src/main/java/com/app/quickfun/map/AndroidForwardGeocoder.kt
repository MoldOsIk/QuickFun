package com.app.quickfun.map

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import java.util.Locale

/**
 * Запасной геокодер устройства (часто работает, когда HTTP Геокодер Яндекса недоступен с ключом MapKit).
 */
object AndroidForwardGeocoder {

    private const val TAG = "AndroidForwardGeocoder"

    fun geocodeBlocking(
        context: Context,
        city: String?,
        address: String?
    ): Pair<Double, Double>? {
        if (!Geocoder.isPresent()) return null
        val q = listOfNotNull(
            address?.trim()?.takeIf { it.isNotEmpty() },
            city?.trim()?.takeIf { it.isNotEmpty() }
        ).joinToString(", ").trim()
        if (q.isEmpty()) return null
        return try {
            val geocoder = Geocoder(context.applicationContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocationName(q, 1)
            if (list.isNullOrEmpty()) null
            else {
                val a = list[0]
                val lat = a.latitude
                val lon = a.longitude
                if (lat == 0.0 && lon == 0.0) null else lat to lon
            }
        } catch (e: Exception) {
            Log.w(TAG, "geocode failed for q=$q (API ${Build.VERSION.SDK_INT})", e)
            null
        }
    }
}
