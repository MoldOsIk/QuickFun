package com.app.quickfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import com.app.quickfun.di.AppModule
import com.app.quickfun.ui.viewmodel.PlaceViewModel
import com.app.quickfun.ui.screens.PlaceScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm = PlaceViewModel(AppModule.getPlacesUseCase)

        setContent {
            PlaceScreen(vm)

            LaunchedEffect(Unit) {
                vm.loadPlaces()
            }
        }
    }
}