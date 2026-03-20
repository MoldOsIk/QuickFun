package com.app.quickfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.quickfun.ui.screens.AuthScreen
import com.app.quickfun.ui.viewmodel.PlaceViewModel
import com.app.quickfun.ui.screens.PlaceScreen
import com.app.quickfun.ui.viewmodel.AuthState
import com.app.quickfun.ui.viewmodel.AuthViewModel
import com.app.quickfun.ui.viewmodel.factory.AuthViewModelFactory
import com.app.quickfun.ui.viewmodel.factory.PlaceViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val authVm: AuthViewModel = viewModel(factory = AuthViewModelFactory())
            val placeVm: PlaceViewModel = viewModel(factory = PlaceViewModelFactory())

            val authState by authVm.state.collectAsState()

            when (authState) {
                is AuthState.Loading -> CircularProgressIndicator()
                is AuthState.Authorized -> PlaceScreen(placeVm)
                else -> AuthScreen(authVm)
            }
        }
    }
}