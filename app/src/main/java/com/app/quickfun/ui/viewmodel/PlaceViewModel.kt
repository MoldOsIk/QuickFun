package com.app.quickfun.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.quickfun.domain.usecase.GetPlacesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.app.quickfun.domain.model.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaceViewModel(
    private val getPlacesUseCase: GetPlacesUseCase
) : ViewModel() {

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places

    init {
        loadPlaces() // ← ВАЖНО
    }

    fun loadPlaces() {
        viewModelScope.launch {
            try {
                _places.value = withContext(Dispatchers.IO) {
                    getPlacesUseCase()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}