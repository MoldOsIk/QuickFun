package com.app.quickfun.map

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray

/**
 * Резервный геокодер [Nominatim](https://nominatim.org/) (OSM).
 * Нужен стабильный User-Agent по [политике использования](https://operations.osmfoundation.org/policies/nominatim/).
 */
object NominatimGeocoder {

    private const val TAG = "NominatimGeocoder"
    private const val USER_AGENT = "QuickFun/1.0 (Android; https://github.com/)"
    private const val MIN_INTERVAL_MS = 1_100L

    private val throttleLock = Any()
    private var lastRequestFinishedMs = 0L

    fun geocodeBlocking(query: String): Pair<Double, Double>? {
        val q = query.trim()
        if (q.isEmpty()) return null
        synchronized(throttleLock) {
            val wait = lastRequestFinishedMs + MIN_INTERVAL_MS - System.currentTimeMillis()
            if (wait > 0) {
                try {
                    Thread.sleep(wait)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            }
        }
        val urlStr = buildString {
            append("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=")
            append(URLEncoder.encode(q, "UTF-8"))
        }
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                .bufferedReader().use { it.readText() }
            conn.disconnect()
            if (code !in 200..299) {
                Log.w(TAG, "HTTP $code q=${q.take(80)} body=${body.take(200)}")
                null
            } else {
                val arr = JSONArray(body)
                if (arr.length() == 0) null
                else {
                    val o = arr.getJSONObject(0)
                    val lat = o.optString("lat", "").toDoubleOrNull()
                    val lon = o.optString("lon", "").toDoubleOrNull()
                    if (lat == null || lon == null) null
                    else {
                        Log.d(TAG, "ok q=${q.take(60)} → $lat,$lon")
                        lat to lon
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "geocode failed q=${q.take(80)}", e)
            null
        } finally {
            synchronized(throttleLock) {
                lastRequestFinishedMs = System.currentTimeMillis()
            }
        }
    }
}
