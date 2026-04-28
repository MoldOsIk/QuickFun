package com.app.quickfun.ui.profile.model

import com.app.quickfun.domain.model.MyActiveBooking

data class ProfileState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSavingPhone: Boolean = false,
    val isRegisteringVenue: Boolean = false,
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val phoneCountryIso: String = "RU",
    val phoneNationalDigits: String = "",
    /** Полный E.164 при корректном наборе; иначе null. */
    val phoneE164Valid: String? = null,
    val roles: List<String> = emptyList(),
    val myActiveBookings: List<MyActiveBooking> = emptyList(),
    val cancellingMyBookingId: Int? = null
)
