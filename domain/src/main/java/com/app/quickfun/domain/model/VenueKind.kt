package com.app.quickfun.domain.model

/**
 * Тип сценария бронирования / настройки мест.
 * Сначала сопоставляется [Place.categoryId] через [VenueCategoryIdSets], иначе — по имени категории.
 */
enum class VenueKind {
    GENERIC,
    BOWLING,
    BILLIARDS,
    CINEMA,
    KARAOKE
}

/**
 * Id из `public.categories` (точнее, чем только имя).
 * По умолчанию: 1=cinema, 2=bowling, 3=billiards, 4=karaoke — как у тебя по порядку вставки.
 * Если в БД другие id — поправь множества здесь.
 */
object VenueCategoryIdSets {
    val cinemaIds: Set<Int> = setOf(1)
    val bowlingIds: Set<Int> = setOf(2)
    val billiardsIds: Set<Int> = setOf(3)
    val karaokeIds: Set<Int> = setOf(4)
}

fun venueKindFromCategoryName(name: String?): VenueKind {
    val n = name?.lowercase()?.trim().orEmpty()
    if (n.isEmpty()) return VenueKind.GENERIC
    return when {
        n.anyKeyword("боулинг", "bowling") -> VenueKind.BOWLING
        n.anyKeyword("бильярд", "billiard", "billiards", "pool", "снукер") -> VenueKind.BILLIARDS
        n.anyKeyword("кино", "кинотеатр", "cinema") -> VenueKind.CINEMA
        n.anyKeyword("караоке", "karaoke") -> VenueKind.KARAOKE
        else -> VenueKind.GENERIC
    }
}

fun Place.resolveVenueKind(): VenueKind {
    val id = categoryId
    if (id != null) {
        when {
            id in VenueCategoryIdSets.cinemaIds -> return VenueKind.CINEMA
            id in VenueCategoryIdSets.bowlingIds -> return VenueKind.BOWLING
            id in VenueCategoryIdSets.billiardsIds -> return VenueKind.BILLIARDS
            id in VenueCategoryIdSets.karaokeIds -> return VenueKind.KARAOKE
        }
    }
    return venueKindFromCategoryName(categoryName)
}

/** Короткое русское название типа для каталога и карты. */
fun VenueKind.ruShortType(): String =
    when (this) {
        VenueKind.CINEMA -> "Кино"
        VenueKind.BOWLING -> "Боулинг"
        VenueKind.BILLIARDS -> "Бильярд"
        VenueKind.KARAOKE -> "Караоке"
        VenueKind.GENERIC -> "Другое"
    }

/**
 * Строка «тип» для UI: по [resolveVenueKind] — по-русски; для GENERIC — сырое имя категории из БД, если есть.
 */
fun Place.displayVenueTypeRu(): String {
    val kind = resolveVenueKind()
    return if (kind != VenueKind.GENERIC) {
        kind.ruShortType()
    } else {
        categoryName?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
    }
}

private fun String.anyKeyword(vararg keys: String): Boolean = keys.any { contains(it) }
