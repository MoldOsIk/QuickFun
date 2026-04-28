package com.app.quickfun.domain.repository

import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.PlaceRegistrationDraft
import com.app.quickfun.domain.model.PlaceReview
import com.app.quickfun.domain.model.VenueCategory

interface PlaceRepository {
    /** Справочник категорий заведений (`public.categories`), доступен по RLS всем. */
    suspend fun getVenueCategories(): List<VenueCategory>

    suspend fun getPlaces(): List<Place>

    /** Только заведения текущего пользователя (по owner_id). */
    suspend fun getMyPlaces(): List<Place>

    /** Заявки на модерацию (только главный admin, RLS). */
    suspend fun getPendingPlacesForModeration(): List<Place>

    /** RPC: заведение в статусе pending + роль place_admin. Возвращает id места. */
    suspend fun registerPlaceAsOwner(draft: PlaceRegistrationDraft): String

    /** RPC: одобрить или отклонить (только главный admin). При отклонении можно передать причину. */
    suspend fun approvePlace(placeId: String, approved: Boolean, rejectionReason: String? = null)

    /** Снова отправить отклонённую заявку на модерацию (владелец). */
    suspend fun resubmitRejectedPlace(placeId: String)

    /** Только для пользователей с ролью admin (RLS). */
    suspend fun createPlace(
        name: String,
        description: String?,
        city: String?,
        address: String?,
        categoryId: Int?,
        latitude: Double?,
        longitude: Double?
    )

    suspend fun updatePlace(
        placeId: String,
        locationId: Int,
        name: String,
        description: String?,
        city: String?,
        address: String?,
        categoryId: Int?,
        latitude: Double?,
        longitude: Double?,
        status: String,
        coverImageUrl: String?
    )

    /** Загрузка в bucket `venue-images`, путь `{placeId}/{uuid}.ext`. Возвращает публичный URL. */
    suspend fun uploadVenueImageToStorage(placeId: String, bytes: ByteArray, fileExtension: String): String

    suspend fun updatePlaceCoverUrl(placeId: String, coverUrl: String?)

    suspend fun insertPlaceGalleryPhoto(placeId: String, imageUrl: String)

    suspend fun deletePlaceGalleryPhoto(photoId: Long)

    /** Отзывы заведения (RLS: одобренные места / своё / админ). */
    suspend fun getPlaceReviews(placeId: String): List<PlaceReview>

    /** Создать или обновить отзыв текущего пользователя (один на пару place+user). */
    suspend fun upsertPlaceReview(placeId: String, rating: Int, body: String)
}