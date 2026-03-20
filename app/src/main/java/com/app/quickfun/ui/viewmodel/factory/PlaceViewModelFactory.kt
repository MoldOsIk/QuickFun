package com.app.quickfun.ui.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.quickfun.di.AppModule
import com.app.quickfun.ui.viewmodel.PlaceViewModel

class PlaceViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlaceViewModel(
            AppModule.getPlacesUseCase
        ) as T
    }
}