package com.app.quickfun.ui.place.model

sealed interface PlaceEffect {
    data class ShowMessage(val message: String) : PlaceEffect
    data object AddPlaceSucceeded : PlaceEffect
    data object EditPlaceSucceeded : PlaceEffect
    /** Схема зала кино сохранена в БД; UI редактора может закрыться. */
    data object CinemaSeatLayoutSaved : PlaceEffect
}
