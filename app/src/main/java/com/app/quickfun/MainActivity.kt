package com.app.quickfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.yandex.mapkit.MapKitFactory
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.app.quickfun.ui.theme.QuickFunTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.quickfun.ui.auth.AuthScreen
import com.app.quickfun.ui.auth.AuthViewModel
import com.app.quickfun.ui.auth.AuthViewModelFactory
import com.app.quickfun.ui.auth.model.AuthIntent
import com.app.quickfun.ui.auth.model.AuthState
import com.app.quickfun.ui.main.MainShell
import com.app.quickfun.ui.place.PlaceViewModel
import com.app.quickfun.ui.place.PlaceViewModelFactory
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.profile.ProfileViewModel
import com.app.quickfun.ui.profile.ProfileViewModelFactory
import com.app.quickfun.ui.profile.model.ProfileIntent

class MainActivity : ComponentActivity() {

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickFunTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authVm: AuthViewModel = viewModel(factory = AuthViewModelFactory())
                    val authState by authVm.state.collectAsState()

                    AnimatedContent(
                        targetState = authState,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "auth_state_transition"
                    ) { current ->
                        when (current) {
                            is AuthState.Loading -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                            is AuthState.Authorized -> {
                                val placeVm: PlaceViewModel =
                                    viewModel(factory = PlaceViewModelFactory(application))
                                val profileVm: ProfileViewModel =
                                    viewModel(factory = ProfileViewModelFactory())
                                // ViewModel привязаны к Activity: init { Load } только при первом создании.
                                // После выхода и повторного входа — явно обновляем каталог, «Моё» и профиль.
                                LaunchedEffect(Unit) {
                                    placeVm.obtainEvent(PlaceIntent.Load)
                                    placeVm.obtainEvent(PlaceIntent.LoadMyPlaces)
                                    profileVm.obtainEvent(ProfileIntent.Refresh)
                                }
                                MainShell(
                                    placeVm = placeVm,
                                    profileVm = profileVm,
                                    onSignOut = { authVm.obtainEvent(AuthIntent.SignOut) }
                                )
                            }
                            else -> AuthScreen(authVm)
                        }
                    }
                }
            }
        }
    }
}