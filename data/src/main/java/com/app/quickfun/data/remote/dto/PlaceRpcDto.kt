@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.app.quickfun.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.Serializable

@Serializable
data class RegisterPlaceAsOwnerRpcParams(
    val p_name: String,
    val p_description: String? = null,
    val p_city: String? = null,
    val p_address: String? = null,
    val p_category_id: Int? = null,
    /** Иначе PostgREST не подбирает сигнатуру `register_place_as_owner(..., double, double)`. */
    @EncodeDefault(ALWAYS)
    val p_latitude: Double? = null,
    @EncodeDefault(ALWAYS)
    val p_longitude: Double? = null
)

@Serializable
data class ApprovePlaceRpcParams(
    val p_place_id: String,
    val p_approved: Boolean,
    @EncodeDefault(ALWAYS)
    val p_rejection_reason: String? = null
)

@Serializable
data class ResubmitPlaceRpcParams(
    val p_place_id: String
)

@Serializable
data class CatalogPlaceCatalogScoreRow(
    val place_id: String,
    val booking_count: Long,
    val review_count: Long,
    val avg_rating: Double? = null,
    val bayes_rating: Double,
    val catalog_sort_score: Double
)
