package com.app.quickfun.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.quickfun.di.AppModule

class AuthViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(
            AppModule.SignInUseCase,
            AppModule.SignUpUseCase,
            AppModule.CheckAuthUseCase,
            AppModule.saveProfileNameUseCase,
            AppModule.registerPlaceAsOwnerUseCase,
            AppModule.signOutUseCase
        ) as T
    }
}
