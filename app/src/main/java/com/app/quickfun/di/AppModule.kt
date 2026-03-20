package com.app.quickfun.di

import com.app.quickfun.data.repository.AuthRepositoryImpl
import com.app.quickfun.data.repository.PlaceRepositoryImpl
import com.app.quickfun.domain.usecase.CheckAuthUseCase
import com.app.quickfun.domain.usecase.GetPlacesUseCase
import com.app.quickfun.domain.usecase.SignInUseCase
import com.app.quickfun.domain.usecase.SignUpUseCase

object AppModule {

    private val Placerepository = PlaceRepositoryImpl()
    private val Authrepository = AuthRepositoryImpl()

    val getPlacesUseCase = GetPlacesUseCase(Placerepository)
    val CheckAuthUseCase = CheckAuthUseCase(Authrepository)
    val SignInUseCase = SignInUseCase(Authrepository)
    val SignUpUseCase = SignUpUseCase(Authrepository)
}