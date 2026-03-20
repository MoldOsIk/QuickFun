package com.app.quickfun.domain.repository

import com.app.quickfun.domain.model.Place

interface PlaceRepository {
    suspend fun getPlaces(): List<Place>
}