package com.app.quickfun.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class PlaceReviewUserNestedDto(
    val name: String? = null
)

@Serializable
data class PlaceReviewRowDto(
    val id: Long,
    @SerialName("place_id") val place_id: String,
    @SerialName("user_id") val user_id: String,
    val rating: Int,
    val body: String,
    @SerialName("created_at") val created_at: String,
    @SerialName("updated_at") val updated_at: String,
    val users: PlaceReviewUserNestedDto? = null
)

@Serializable
data class PlaceReviewInsertDto(
    @SerialName("place_id") val place_id: String,
    @SerialName("user_id") val user_id: String,
    val rating: Int,
    val body: String
)

@Serializable
data class PlaceReviewUpdateDto(
    val rating: Int,
    val body: String,
    @SerialName("updated_at") val updated_at: String
)
