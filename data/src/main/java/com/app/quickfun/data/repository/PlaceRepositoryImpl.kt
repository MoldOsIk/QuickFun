package com.app.quickfun.data.repository

import com.app.quickfun.data.mapper.toDomain
import com.app.quickfun.data.remote.SupabaseClient
import com.app.quickfun.data.remote.dto.ApprovePlaceRpcParams
import com.app.quickfun.data.remote.dto.CatalogPlaceCatalogScoreRow
import com.app.quickfun.data.remote.dto.CategoryRowDto
import com.app.quickfun.data.remote.dto.toVenueCategory
import com.app.quickfun.data.remote.dto.LocationIdRow
import com.app.quickfun.data.remote.dto.LocationInsertDto
import com.app.quickfun.data.remote.dto.PlaceDto
import com.app.quickfun.data.remote.dto.LocationUpdateDto
import com.app.quickfun.data.remote.dto.IdLongRow
import com.app.quickfun.data.remote.dto.PlaceCoverOnlyUpdateDto
import com.app.quickfun.data.remote.dto.PlaceGalleryInsertDto
import com.app.quickfun.data.remote.dto.PlaceInsertDto
import com.app.quickfun.data.remote.dto.PlaceUpdateDto
import com.app.quickfun.data.remote.dto.PlaceReviewInsertDto
import com.app.quickfun.data.remote.dto.PlaceReviewRowDto
import com.app.quickfun.data.remote.dto.PlaceReviewUpdateDto
import com.app.quickfun.data.remote.dto.RegisterPlaceAsOwnerRpcParams
import com.app.quickfun.data.remote.dto.ResubmitPlaceRpcParams
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.PlaceRegistrationDraft
import com.app.quickfun.domain.model.PlaceReview
import com.app.quickfun.domain.model.VenueCategory
import com.app.quickfun.domain.repository.PlaceRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.time.Instant
import java.util.UUID

/** supabase-kt объявляет rpc с параметрами как inline — вызывать только из inline-контекста. */
private suspend inline fun <reified P : Any> postgrestRpc(
    function: String,
    parameters: P
): PostgrestResult = SupabaseClient.client.postgrest.rpc(function, parameters)

private val catalogRpcJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private data class CatalogPlaceScoreBundle(
    val bookingCount: Int,
    val reviewCount: Int,
    val avgRating: Double?,
    val bayesRating: Double,
    val sortScore: Double
)

private suspend fun fetchCatalogCatalogScores(): Map<String, CatalogPlaceScoreBundle> {
    return try {
        val result = SupabaseClient.client.postgrest.rpc("catalog_place_catalog_scores")
        val rows = catalogRpcJson.decodeFromString(
            ListSerializer(CatalogPlaceCatalogScoreRow.serializer()),
            result.data
        )
        rows.associate { row ->
            row.place_id to CatalogPlaceScoreBundle(
                bookingCount = row.booking_count.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                reviewCount = row.review_count.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                avgRating = row.avg_rating,
                bayesRating = row.bayes_rating,
                sortScore = row.catalog_sort_score
            )
        }
    } catch (_: Exception) {
        emptyMap()
    }
}

private const val PLACE_SELECT = """
    id,
    name,
    description,
    status,
    owner_id,
    location_id,
    category_id,
    rejection_reason,
    cover_image_url,
    place_gallery(id, image_url, sort_order),
    categories:category_id(name),
    locations:location_id(latitude, longitude, address, city)
"""

class PlaceRepositoryImpl : PlaceRepository {

    override suspend fun getVenueCategories(): List<VenueCategory> {
        SupabaseClient.client.auth.loadFromStorage()
        return SupabaseClient.client
            .from("categories")
            .select(Columns.raw("id, name"))
            .decodeList<CategoryRowDto>()
            .map { it.toVenueCategory() }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
    }

    override suspend fun getPlaces(): List<Place> {
        SupabaseClient.client.auth.loadFromStorage()

        // Без filter в PostgREST: кто что видит, задаёт только RLS (migration_v2 places_select).
        // Клиентский or(eq status, eq owner_id) давал пустой список у части пользователей из‑за
        // несовпадения формата UUID / разбора or в PostgREST.
        val scores = fetchCatalogCatalogScores()
        return SupabaseClient.client
            .from("places")
            .select(Columns.raw(PLACE_SELECT))
            .decodeList<PlaceDto>()
            .map { it.toDomain() }
            .map { p ->
                val s = scores[p.id]
                if (s == null) {
                    p
                } else {
                    p.copy(
                        lifetimeBookingCount = s.bookingCount,
                        reviewCount = s.reviewCount,
                        avgReviewRating = s.avgRating,
                        bayesReviewRating = s.bayesRating,
                        catalogSortScore = s.sortScore
                    )
                }
            }
            .sortedWith(
                compareByDescending<Place> { it.catalogSortScore }
                    .thenBy { it.name.lowercase() }
            )
    }

    override suspend fun getMyPlaces(): List<Place> {
        SupabaseClient.client.auth.loadFromStorage()
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
            ?: return emptyList()
        return SupabaseClient.client
            .from("places")
            .select(Columns.raw(PLACE_SELECT)) {
                filter {
                    eq("owner_id", uid)
                }
            }
            .decodeList<PlaceDto>()
            .map { it.toDomain() }
            .sortedBy { it.name }
    }

    override suspend fun getPendingPlacesForModeration(): List<Place> {
        SupabaseClient.client.auth.loadFromStorage()
        return SupabaseClient.client
            .from("places")
            .select(Columns.raw(PLACE_SELECT)) {
                filter {
                    eq("status", "pending")
                }
            }
            .decodeList<PlaceDto>()
            .map { it.toDomain() }
            .sortedBy { it.name }
    }

    override suspend fun registerPlaceAsOwner(draft: PlaceRegistrationDraft): String {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No active session")
        }
        val params = RegisterPlaceAsOwnerRpcParams(
            p_name = draft.name.trim(),
            p_description = draft.description?.trim()?.takeIf { it.isNotEmpty() },
            p_city = draft.city?.trim()?.takeIf { it.isNotEmpty() },
            p_address = draft.address?.trim()?.takeIf { it.isNotEmpty() },
            p_category_id = draft.categoryId,
            p_latitude = draft.latitude,
            p_longitude = draft.longitude
        )
        val result = postgrestRpc(
            function = "register_place_as_owner",
            parameters = params
        )
        return Json.decodeFromString(serializer<String>(), result.data)
    }

    override suspend fun approvePlace(placeId: String, approved: Boolean, rejectionReason: String?) {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No active session")
        }
        postgrestRpc(
            function = "approve_place",
            parameters = ApprovePlaceRpcParams(
                p_place_id = placeId,
                p_approved = approved,
                p_rejection_reason = rejectionReason?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    override suspend fun resubmitRejectedPlace(placeId: String) {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No active session")
        }
        postgrestRpc(
            function = "resubmit_place_after_rejection",
            parameters = ResubmitPlaceRpcParams(p_place_id = placeId)
        )
    }

    override suspend fun createPlace(
        name: String,
        description: String?,
        city: String?,
        address: String?,
        categoryId: Int?,
        latitude: Double?,
        longitude: Double?
    ) {
        SupabaseClient.client.auth.loadFromStorage()
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No active session")

        val locationRow = SupabaseClient.client
            .from("locations")
            .insert(
                LocationInsertDto(
                    latitude = latitude,
                    longitude = longitude,
                    address = address,
                    city = city
                )
            ) {
                select(Columns.list("id"))
            }
            .decodeSingle<LocationIdRow>()

        SupabaseClient.client.from("places").insert(
            PlaceInsertDto(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                category_id = categoryId,
                location_id = locationRow.id,
                owner_id = uid,
                status = "approved"
            )
        )
    }

    override suspend fun updatePlace(
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
    ) {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No active session")
        }

        SupabaseClient.client.from("locations").update(
            LocationUpdateDto(
                latitude = latitude,
                longitude = longitude,
                address = address,
                city = city
            )
        ) {
            filter {
                eq("id", locationId)
            }
        }

        SupabaseClient.client.from("places").update(
            PlaceUpdateDto(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                category_id = categoryId,
                status = status,
                cover_image_url = coverImageUrl
            )
        ) {
            filter {
                eq("id", placeId)
            }
        }
    }

    override suspend fun uploadVenueImageToStorage(
        placeId: String,
        bytes: ByteArray,
        fileExtension: String
    ): String {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No active session")
        }
        if (bytes.isEmpty()) {
            throw IllegalArgumentException("Пустой файл.")
        }
        val ext = fileExtension.trim().removePrefix(".").lowercase().ifEmpty { "jpg" }
        val safeExt = when (ext) {
            "jpeg", "jpg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            else -> "jpg"
        }
        val path = "$placeId/${UUID.randomUUID()}.$safeExt"
        val bucket = SupabaseClient.client.storage.from(SupabaseClient.VENUE_IMAGES_BUCKET)
        bucket.upload(path, bytes, upsert = false)
        return bucket.publicUrl(path)
    }

    override suspend fun updatePlaceCoverUrl(placeId: String, coverUrl: String?) {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No active session")
        }
        SupabaseClient.client.from("places").update(
            PlaceCoverOnlyUpdateDto(cover_image_url = coverUrl)
        ) {
            filter { eq("id", placeId) }
        }
    }

    override suspend fun insertPlaceGalleryPhoto(placeId: String, imageUrl: String) {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No active session")
        }
        val existing = SupabaseClient.client
            .from("place_gallery")
            .select(Columns.list("id")) {
                filter { eq("place_id", placeId) }
            }
            .decodeList<IdLongRow>()
        if (existing.size >= 3) {
            throw IllegalStateException("В галерее не больше 3 фото.")
        }
        SupabaseClient.client.from("place_gallery").insert(
            PlaceGalleryInsertDto(
                place_id = placeId,
                image_url = imageUrl,
                sort_order = existing.size
            )
        )
    }

    override suspend fun deletePlaceGalleryPhoto(photoId: Long) {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No active session")
        }
        SupabaseClient.client.from("place_gallery").delete {
            filter { eq("id", photoId) }
        }
    }

    override suspend fun getPlaceReviews(placeId: String): List<PlaceReview> {
        SupabaseClient.client.auth.loadFromStorage()
        val rows = SupabaseClient.client
            .from("place_reviews")
            .select(
                Columns.raw(
                    """
                    id,
                    place_id,
                    user_id,
                    rating,
                    body,
                    created_at,
                    updated_at,
                    users(name)
                    """.trimIndent()
                )
            ) {
                filter {
                    eq("place_id", placeId)
                }
            }
            .decodeList<PlaceReviewRowDto>()
        return rows
            .map { it.toDomain() }
            .sortedWith(compareByDescending<PlaceReview> { it.createdAtIso })
    }

    override suspend fun upsertPlaceReview(placeId: String, rating: Int, body: String) {
        SupabaseClient.client.auth.loadFromStorage()
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No active session")
        val trimmed = body.trim()
        if (trimmed.length < 3) {
            throw IllegalArgumentException("Текст отзыва не короче 3 символов.")
        }
        if (rating !in 1..10) {
            throw IllegalArgumentException("Оценка от 1 до 10.")
        }
        val existing = SupabaseClient.client
            .from("place_reviews")
            .select(Columns.list("id")) {
                filter {
                    eq("place_id", placeId)
                    eq("user_id", uid)
                }
            }
            .decodeList<IdLongRow>()
        if (existing.isEmpty()) {
            SupabaseClient.client.from("place_reviews").insert(
                PlaceReviewInsertDto(
                    place_id = placeId,
                    user_id = uid,
                    rating = rating,
                    body = trimmed
                )
            )
        } else {
            SupabaseClient.client.from("place_reviews").update(
                PlaceReviewUpdateDto(
                    rating = rating,
                    body = trimmed,
                    updated_at = Instant.now().toString()
                )
            ) {
                filter {
                    eq("place_id", placeId)
                    eq("user_id", uid)
                }
            }
        }
    }
}
