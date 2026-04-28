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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import com.app.quickfun.ui.place.BookingPlaceDialog
import com.app.quickfun.ui.place.ModerationTab
import com.app.quickfun.ui.place.MyVenueTab
import com.app.quickfun.ui.place.PlaceCatalogTab
import com.app.quickfun.ui.place.PlaceMapTab
import com.app.quickfun.ui.place.PlaceReviewsBottomSheet
import com.app.quickfun.ui.place.PlaceViewModel
import com.app.quickfun.ui.place.model.PlaceEffect
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.profile.ProfileScreen
import com.app.quickfun.ui.profile.ProfileViewModel
import com.app.quickfun.ui.profile.model.ProfileIntent

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
    val placeState by placeVm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    /** После закрытия диалога брони обновляем профиль (список «Мои брони»). */
    var hadBookingDialogOpen by remember { mutableStateOf(false) }
    /** Чтобы не удерживать пользователя на «Карте», если pending уже отработал переключением вкладки. */
    var lastAutoSwitchedMapForPendingId by remember { mutableStateOf<String?>(null) }

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

    LaunchedEffect(placeState.pendingMapFocusPlaceId, tabs) {
        val pendingId = placeState.pendingMapFocusPlaceId
        if (pendingId == null) {
            lastAutoSwitchedMapForPendingId = null
            return@LaunchedEffect
        }
        if (pendingId == lastAutoSwitchedMapForPendingId) return@LaunchedEffect
        lastAutoSwitchedMapForPendingId = pendingId
        val mapIndex = tabs.indexOf(MainTab.Map)
        if (mapIndex >= 0) selectedTab = mapIndex
    }

    LaunchedEffect(currentTab, isSuperAdmin) {
        if (currentTab == MainTab.Moderation && isSuperAdmin) {
            placeVm.obtainEvent(PlaceIntent.LoadModeration)
        }
    }

    /** Счётчик заявок на боттом-баре: первая загрузка и периодическое обновление без захода на «Модерация». */
    LaunchedEffect(isSuperAdmin, profile.isLoading) {
        if (!isSuperAdmin || profile.isLoading) return@LaunchedEffect
        while (true) {
            placeVm.obtainEvent(PlaceIntent.LoadModeration)
            delay(45_000L)
        }
    }

    /**
     * Пока открыта «Моё», периодически обновляем список заведений.
     * Тогда при одобрении/отклонении модератором срабатывает сравнение в [PlaceViewModel.reloadMyPlaces]
     * и snackbar показывается прямо на этой вкладке, без ручного обновления.
     */
    LaunchedEffect(currentTab, isPlaceAdmin, profile.isLoading) {
        if (currentTab != MainTab.MyVenue || !isPlaceAdmin || profile.isLoading) return@LaunchedEffect
        while (true) {
            placeVm.obtainEvent(PlaceIntent.LoadMyPlaces)
            delay(45_000L)
        }
    }

    LaunchedEffect(placeState.bookingPlace) {
        val open = placeState.bookingPlace != null
        if (hadBookingDialogOpen && !open) {
            profileVm.obtainEvent(ProfileIntent.Refresh)
        }
        hadBookingDialogOpen = open
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
                        icon = {
                            if (tab == MainTab.Moderation && isSuperAdmin) {
                                val pending = placeState.pendingPlaces.size
                                BadgedBox(
                                    badge = {
                                        if (pending > 0) {
                                            Badge {
                                                Text(
                                                    if (pending > 9) "9+"
                                                    else pending.toString()
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(icon, contentDescription = label)
                                }
                            } else {
                                Icon(icon, contentDescription = label)
                            }
                        },
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

            if (placeState.bookingPlace != null) {
                BookingPlaceDialog(placeVm = placeVm)
            }
            if (placeState.reviewsPlaceId != null) {
                PlaceReviewsBottomSheet(placeVm = placeVm, profile = profile)
            }
        }
    }
}
