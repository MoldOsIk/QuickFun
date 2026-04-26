package com.app.quickfun.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.app.quickfun.ui.place.BookingPlaceDialog
import com.app.quickfun.ui.place.ModerationTab
import com.app.quickfun.ui.place.MyVenueTab
import com.app.quickfun.ui.place.PlaceCatalogTab
import com.app.quickfun.ui.place.PlaceMapTab
import com.app.quickfun.ui.place.PlaceViewModel
import com.app.quickfun.ui.place.model.PlaceEffect
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.profile.ProfileScreen
import com.app.quickfun.ui.profile.ProfileViewModel

private enum class MainTab {
    Catalog,
    Map,
    MyVenue,
    Moderation,
    Profile
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    placeVm: PlaceViewModel,
    profileVm: ProfileViewModel,
    onSignOut: () -> Unit
) {
    val profile by profileVm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    val isSuperAdmin = !profile.isLoading &&
        profile.roles.any { it.equals("admin", ignoreCase = true) }
    val isPlaceAdmin = !profile.isLoading &&
        profile.roles.any { it.equals("place_admin", ignoreCase = true) }

    val tabs: List<MainTab> = buildList {
        add(MainTab.Catalog)
        add(MainTab.Map)
        if (isPlaceAdmin) add(MainTab.MyVenue)
        if (isSuperAdmin) add(MainTab.Moderation)
        add(MainTab.Profile)
    }

    LaunchedEffect(tabs, isSuperAdmin, isPlaceAdmin) {
        if (selectedTab >= tabs.size) {
            selectedTab = 0
        }
    }

    val currentTab = tabs.getOrElse(selectedTab) { MainTab.Catalog }

    LaunchedEffect(currentTab, isSuperAdmin) {
        if (currentTab == MainTab.Moderation && isSuperAdmin) {
            placeVm.obtainEvent(PlaceIntent.LoadModeration)
        }
    }

    LaunchedEffect(currentTab, isPlaceAdmin) {
        if (currentTab == MainTab.MyVenue && isPlaceAdmin) {
            placeVm.obtainEvent(PlaceIntent.LoadMyPlaces)
        }
    }

    LaunchedEffect(currentTab) {
        if (currentTab == MainTab.Map) {
            placeVm.obtainEvent(PlaceIntent.Load)
        }
    }

    LaunchedEffect(placeVm) {
        placeVm.effects.collect { effect ->
            if (effect is PlaceEffect.ShowMessage) {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "QuickFun",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    val label = when (tab) {
                        MainTab.Catalog -> "Каталог"
                        MainTab.Map -> "Карта"
                        MainTab.MyVenue -> "Моё"
                        MainTab.Moderation -> "Модерация"
                        MainTab.Profile -> "Профиль"
                    }
                    val icon = when (tab) {
                        MainTab.Catalog -> Icons.Filled.Home
                        MainTab.Map -> Icons.Filled.Map
                        MainTab.MyVenue -> Icons.Filled.LocationOn
                        MainTab.Moderation -> Icons.AutoMirrored.Filled.List
                        MainTab.Profile -> Icons.Filled.Person
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentTab) {
                MainTab.Catalog -> PlaceCatalogTab(
                    placeVm = placeVm,
                    profile = profile,
                    snackbarHostState = snackbarHostState
                )
                MainTab.Map -> PlaceMapTab(placeVm = placeVm)
                MainTab.MyVenue -> MyVenueTab(
                    placeVm = placeVm,
                    profile = profile,
                    snackbarHostState = snackbarHostState
                )
                MainTab.Moderation -> ModerationTab(placeVm = placeVm)
                MainTab.Profile -> ProfileScreen(
                    profileVm = profileVm,
                    snackbarHostState = snackbarHostState,
                    onSignOut = onSignOut
                )
            }

            val placeState by placeVm.state.collectAsState()
            if (placeState.bookingPlace != null) {
                BookingPlaceDialog(placeVm = placeVm)
            }
        }
    }
}
