package com.app.quickfun.ui.profile.model

sealed interface ProfileEffect {
    data class ShowMessage(val message: String) : ProfileEffect
    data object VenueRegistered : ProfileEffect
}
