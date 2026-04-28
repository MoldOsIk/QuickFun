package com.app.quickfun.domain.model

data class VenueCategory(
    val id: Int,
    val name: String
) {
    /**
     * Подпись в списке выбора: сначала известные [VenueCategoryIdSets], затем эвристика по [name] (как у [Place.resolveVenueKind]).
     */
    fun displayNameRu(): String {
        when {
            id in VenueCategoryIdSets.cinemaIds -> return "Кино"
            id in VenueCategoryIdSets.bowlingIds -> return "Боулинг"
            id in VenueCategoryIdSets.billiardsIds -> return "Бильярд"
            id in VenueCategoryIdSets.karaokeIds -> return "Караоке"
        }
        return when (venueKindFromCategoryName(name)) {
            VenueKind.CINEMA -> "Кино"
            VenueKind.BOWLING -> "Боулинг"
            VenueKind.BILLIARDS -> "Бильярд"
            VenueKind.KARAOKE -> "Караоке"
            VenueKind.GENERIC -> name.trim().ifEmpty { "Другое" }
        }
    }
}
