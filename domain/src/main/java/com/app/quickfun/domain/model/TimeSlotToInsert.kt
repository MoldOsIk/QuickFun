package com.app.quickfun.domain.model

/** Один интервал в `time_slots` (UTC ISO без Z, как в остальном коде). */
data class TimeSlotToInsert(
    val startTimeUtcIso: String,
    val endTimeUtcIso: String,
    val sessionLabel: String? = null
)
