package com.app.quickfun.domain.model

data class TimeSlot(
    val id: Int,
    val placeId: String,
    val startTimeIso: String,
    val endTimeIso: String,
    val sessionLabel: String? = null
)
