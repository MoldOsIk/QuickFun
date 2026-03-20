package com.app.quickfun.ui.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.quickfun.di.AppModule
import com.app.quickfun.ui.viewmodel.AuthViewModel

class AuthViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(
            AppModule.SignInUseCase,
            AppModule.SignUpUseCase,
            AppModule.CheckAuthUseCase
        ) as T
    }
}