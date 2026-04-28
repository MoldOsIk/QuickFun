package com.app.quickfun.domain.model

data class Place(
    val id: String,
    val name: String,
    val description: String?,
    val status: String?,
    val ownerId: String? = null,
    val categoryName: String?,
    val city: String?,
    val address: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationId: Int? = null,
    val categoryId: Int? = null,
    val coverImageUrl: String? = null,
    val galleryPhotos: List<PlacePhoto> = emptyList(),
    /** Всего броней за всё время (для сортировки каталога). */
    val lifetimeBookingCount: Int = 0,
    /** Число отзывов (агрегат для каталога). */
    val reviewCount: Int = 0,
    /** Средняя оценка по отзывам (1–10), null если отзывов нет. */
    val avgReviewRating: Double? = null,
    /**
     * Сглаженный рейтинг для сортировки и порога «мин. рейтинг»:
     * (C·m + Σr) / (C + n), C=8, m=5.5.
     */
    val bayesReviewRating: Double = 5.5,
    /** ln(1 + брони) × bayes — основной ключ сортировки каталога. */
    val catalogSortScore: Double = 0.0,
    /** Комментарий главного админа при отклонении заявки. */
    val rejectionReason: String? = null
)