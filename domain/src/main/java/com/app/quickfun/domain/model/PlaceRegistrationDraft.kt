package com.app.quickfun.domain.model

data class PlaceRegistrationDraft(
    val name: String,
    val description: String?,
    val city: String?,
    val address: String?,
    val categoryId: Int?,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Либо обе координаты заданы, либо обе пусты — тогда нужен город или адрес (для карты/геокодера).
 */
fun coordinatesOrAddressError(
    latitude: Double?,
    longitude: Double?,
    city: String?,
    address: String?
): String? = when {
    latitude != null && longitude != null -> null
    latitude == null && longitude == null ->
        if (city.isNullOrBlank() && address.isNullOrBlank()) {
            "Укажите город или адрес, если координаты не заполнены."
        } else {
            null
        }
    else -> "Укажите обе координаты или очистите оба поля."
}

fun PlaceRegistrationDraft.coordinatesOrAddressMessage(): String? =
    coordinatesOrAddressError(latitude, longitude, city, address)
