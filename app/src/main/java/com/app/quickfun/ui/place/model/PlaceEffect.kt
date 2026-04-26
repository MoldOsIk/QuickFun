package com.app.quickfun.ui.place.model

sealed interface PlaceEffect {
    data class ShowMessage(val message: String) : PlaceEffect
    data object AddPlaceSucceeded : PlaceEffect
    data object EditPlaceSucceeded : PlaceEffect
}
