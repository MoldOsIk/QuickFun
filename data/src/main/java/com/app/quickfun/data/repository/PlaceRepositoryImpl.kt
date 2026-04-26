package com.app.quickfun.data.repository

import com.app.quickfun.data.mapper.toDomain
import com.app.quickfun.data.remote.SupabaseClient
import com.app.quickfun.data.remote.dto.ApprovePlaceRpcParams
import com.app.quickfun.data.remote.dto.LocationIdRow
import com.app.quickfun.data.remote.dto.LocationInsertDto
import com.app.quickfun.data.remote.dto.PlaceDto
import com.app.quickfun.data.remote.dto.LocationUpdateDto
import com.app.quickfun.data.remote.dto.IdLongRow
import com.app.quickfun.data.remote.dto.PlaceCoverOnlyUpdateDto
import com.app.quickfun.data.remote.dto.PlaceGalleryInsertDto
import com.app.quickfun.data.remote.dto.PlaceInsertDto
import com.app.quickfun.data.remote.dto.PlaceUpdateDto
import com.app.quickfun.data.remote.dto.RegisterPlaceAsOwnerRpcParams
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.PlaceRegistrationDraft
import com.app.quickfun.domain.repository.PlaceRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.UUID

/** supabase-kt объявляет rpc с параметрами как inline — вызывать только из inline-контекста. */
private suspend inline fun <reified P : Any> postgrestRpc(
    function: String,
    parameters: P
): PostgrestResult = SupabaseClient.client.postgrest.rpc(function, parameters)

private const val PLACE_SELECT = """
    id,
    name,
    description,
    status,
    owner_id,
    location_id,
    category_id,
    cover_image_url,
    place_gallery(id, image_url, sort_order),
    categories:category_id(name),
    locations:location_id(latitude, longitude, address, city)
"""

class PlaceRepositoryImpl : PlaceRepository {

    override suspend fun getPlaces(): List<Place> {
        SupabaseClient.client.auth.loadFromStorage()

        // Без filter в PostgREST: кто что видит, задаёт только RLS (migration_v2 places_select).
        // Клиентский or(eq status, eq owner_id) давал пустой список у части пользователей из‑за
        // несовпадения формата UUID / разбора or в PostgREST.
        return SupabaseClient.client
            .from("places")
            .select(Columns.raw(PLACE_SELECT))
            .decodeList<PlaceDto>()
            .map { it.toDomain() }
            .sortedBy { it.name }
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

    override suspend fun approvePlace(placeId: String, approved: Boolean) {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No active session")
        }
        postgrestRpc(
            function = "approve_place",
            parameters = ApprovePlaceRpcParams(
                p_place_id = placeId,
                p_approved = approved
            )
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
}
