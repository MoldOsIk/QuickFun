package com.app.quickfun.di

import com.app.quickfun.data.repository.PlaceRepositoryImpl
import com.app.quickfun.domain.usecase.GetPlacesUseCase

object AppModule {

    private val repository = PlaceRepositoryImpl()

    val getPlacesUseCase = GetPlacesUseCase(repository)
}