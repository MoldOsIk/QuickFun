package com.app.quickfun.ui.profile.model

import com.app.quickfun.domain.model.PlaceRegistrationDraft

sealed interface ProfileIntent {
    data object Refresh : ProfileIntent
    data class NameChanged(val value: String) : ProfileIntent
    data object SaveName : ProfileIntent
    data class RegisterVenue(val draft: PlaceRegistrationDraft) : ProfileIntent
}
