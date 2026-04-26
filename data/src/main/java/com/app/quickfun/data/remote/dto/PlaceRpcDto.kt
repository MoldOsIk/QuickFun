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
    val p_approved: Boolean
)
