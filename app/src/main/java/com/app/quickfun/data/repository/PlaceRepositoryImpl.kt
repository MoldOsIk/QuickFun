package com.app.quickfun.data.repository

import com.app.quickfun.data.mapper.toDomain
import com.app.quickfun.data.remote.SupabaseClient
import com.app.quickfun.data.remote.dto.PlaceDto
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.repository.PlaceRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class PlaceRepositoryImpl : PlaceRepository {

    override suspend fun getPlaces(): List<Place> {
        return SupabaseClient.client
            .from("places")
            .select(
                Columns.raw(
                    """
                    id,
                    name,
                    description,
                    status,
                    categories:category_id(name),
                    locations:location_id(address, city)
                    """
                )
            ) {
                filter {
                    eq("status", "approved")
                }
            }
            .decodeList<PlaceDto>()
            .map { it.toDomain() }
    }
}