package com.app.quickfun.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.quickfun.di.AppModule

class ProfileViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(
            AppModule.getProfileUseCase,
            AppModule.saveProfileNameUseCase,
            AppModule.registerPlaceAsOwnerUseCase
        ) as T
    }
}
