package com.app.quickfun.ui.profile.model

import com.app.quickfun.domain.model.PlaceRegistrationDraft

sealed interface ProfileIntent {
    data object Refresh : ProfileIntent
    data class NameChanged(val value: String) : ProfileIntent
    data object SaveName : ProfileIntent
    data class PhoneCountryIsoChanged(val iso: String) : ProfileIntent
    data class PhoneNationalDigitsChanged(val value: String) : ProfileIntent
    data class PhoneE164ValidityChanged(val e164: String?) : ProfileIntent
    data object SavePhone : ProfileIntent
    data object ClearPhone : ProfileIntent
    data class CancelMyBooking(val bookingId: Int) : ProfileIntent
    data class RegisterVenue(val draft: PlaceRegistrationDraft) : ProfileIntent
}
