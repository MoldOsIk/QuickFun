package com.app.quickfun.map

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Прямое геокодирование (адрес → координаты) через HTTP API Яндекса.
 * Используйте тот же ключ, что и для MapKit ([local.properties]: MAPKIT_API_KEY).
 * В кабинете разработчика для ключа должны быть разрешены запросы к Геокодеру.
 */
object YandexGeocoder {

    private const val TAG = "YandexGeocoder"

    /** Ключ MapKit часто не подходит к REST Геокодеру → 403; дальше не дёргаем API зря. */
    @Volatile
    private var skipHttpGeocode: Boolean = false

    private fun buildQuery(city: String?, address: String?): String? =
        listOfNotNull(
            city?.trim()?.takeIf { it.isNotEmpty() },
            address?.trim()?.takeIf { it.isNotEmpty() }
        ).joinToString(", ").trim().takeIf { it.isNotEmpty() }

    private fun geocodeBlocking(apiKey: String, query: String): Pair<Double, Double>? {
        if (skipHttpGeocode) return null
        val key = apiKey.trim()
        if (key.isEmpty() || query.isEmpty()) return null
        val url = buildString {
            append("https://geocode-maps.yandex.ru/1.x/?format=json&results=1")
            append("&apikey=").append(URLEncoder.encode(key, "UTF-8"))
            append("&geocode=").append(URLEncoder.encode(query, "UTF-8"))
        }
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                .bufferedReader().use { it.readText() }
            conn.disconnect()
            if (code !in 200..299) {
                if (code == 403) {
                    val low = body.lowercase(Locale.ROOT)
                    if (low.contains("invalid api key") || low.contains("\"forbidden\"")) {
                        if (!skipHttpGeocode) {
                            skipHttpGeocode = true
                            Log.w(
                                TAG,
                                "Ключ MapKit не подходит к HTTP Геокодеру Яндекса (403). " +
                                    "Дальше используются только Android Geocoder и Nominatim. " +
                                    "Для Яндекса нужен отдельный ключ с доступом к Geocoder API."
                            )
                        }
                    }
                }
                Log.w(TAG, "HTTP $code for geocode query=$query body=${body.take(200)}")
                null
            } else {
                parseFirstPoint(body)
            }
        } catch (e: Exception) {
            Log.e(TAG, "forwardGeocode failed for q=$query", e)
            null
        }
    }

    /**
     * @return пара (широта, долгота) в WGS84 или null, если не нашли / ошибка сети.
     */
    suspend fun forwardGeocode(
        apiKey: String,
        city: String?,
        address: String?
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (skipHttpGeocode) return@withContext null
        val q = buildQuery(city, address) ?: return@withContext null
        geocodeBlocking(apiKey, q)
    }

    /**
     * Если «город, улица» неоднозначны, повторяем с уточнением страны (частый случай РБ без координат в БД).
     */
    suspend fun forwardGeocodeWithRegionRetries(
        apiKey: String,
        city: String?,
        address: String?
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (skipHttpGeocode) return@withContext null
        val key = apiKey.trim()
        if (key.isEmpty()) return@withContext null
        val base = buildQuery(city, address) ?: return@withContext null
        geocodeBlocking(key, base)?.let { return@withContext it }
        val lower = base.lowercase(Locale.ROOT)
        val hasCountry = lower.contains("беларус") || lower.contains("belarus") ||
            lower.contains("росси") || lower.contains("russia") ||
            lower.contains("украин") || lower.contains("ukraine") ||
            lower.contains("казах") || lower.contains("kazakhstan")
        if (!hasCountry) {
            geocodeBlocking(key, "$base, Беларусь")?.let { return@withContext it }
            geocodeBlocking(key, "$base, Россия")?.let { return@withContext it }
        }
        null
    }

    private fun parseFirstPoint(json: String): Pair<Double, Double>? {
        val root = JSONObject(json)
        val collection = root
            .optJSONObject("response")
            ?.optJSONObject("GeoObjectCollection") ?: return null
        val members = collection.optJSONArray("featureMember") ?: return null
        if (members.length() == 0) return null
        val point = members
            .optJSONObject(0)
            ?.optJSONObject("GeoObject")
            ?.optJSONObject("Point") ?: return null
        val pos = point.optString("pos", "").trim().ifEmpty { return null }
        val parts = pos.split(Regex("\\s+"))
        if (parts.size < 2) return null
        val lon = parts[0].toDoubleOrNull() ?: return null
        val lat = parts[1].toDoubleOrNull() ?: return null
        return lat to lon
    }
}
