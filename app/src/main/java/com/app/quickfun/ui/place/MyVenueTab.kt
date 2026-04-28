package com.app.quickfun.ui.place

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.Place
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.profile.model.ProfileState

/**
 * Отдельный экран только заведений текущего владельца (owner_id).
 * Каталог по-прежнему показывает «все для пользователя» (одобренные + свои).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVenueTab(
    placeVm: PlaceViewModel,
    profile: ProfileState,
    snackbarHostState: SnackbarHostState
) {
    val placeState by placeVm.state.collectAsState()
    var editTarget by remember { mutableStateOf<Place?>(null) }
    var myVenueDetailPlaceId by remember { mutableStateOf<String?>(null) }

    val isSuperAdmin = !profile.isLoading &&
        profile.roles.any { it.equals("admin", ignoreCase = true) }

    fun canEdit(place: Place): Boolean {
        if (isSuperAdmin) return true
        val uid = profile.userId
        if (uid.isEmpty() || place.ownerId == null) return false
        return place.ownerId == uid
    }

    when {
        placeState.isLoadingMyPlaces && placeState.myPlaces.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        placeState.myPlaces.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Пока нет ваших заведений",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Зарегистрируйте заведение при регистрации или в профиле — после одобрения оно появится здесь и в каталоге.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Здесь только ваши площадки: «Расписание» — слоты и места, «Брони» — кто забронировал.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(placeState.myPlaces, key = { it.id }) { place ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = place.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (canEdit(place) && place.locationId != null) {
                                        TextButton(onClick = { editTarget = place }) {
                                            Text("Изменить")
                                        }
                                    }
                                }
                                val st = place.status?.lowercase()
                                if (st != null && st != "approved") {
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                when (st) {
                                                    "pending" -> "На модерации"
                                                    "rejected" -> "Отклонено"
                                                    else -> st
                                                }
                                            )
                                        }
                                    )
                                    if (st == "rejected") {
                                        val reason = place.rejectionReason?.trim()
                                        if (!reason.isNullOrEmpty()) {
                                            Text(
                                                text = "Причина: $reason",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                        if (canEdit(place)) {
                                            FilledTonalButton(
                                                onClick = {
                                                    placeVm.obtainEvent(
                                                        PlaceIntent.ResubmitRejectedPlace(place.id)
                                                    )
                                                },
                                                enabled = !placeState.isLoadingMyPlaces,
                                                modifier = Modifier.padding(top = 8.dp)
                                            ) {
                                                Text("Снова на модерацию")
                                            }
                                        }
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { myVenueDetailPlaceId = place.id }
                                ) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    PlaceVenueCoverOnly(place = place, height = 112.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = place.description?.takeIf { it.isNotBlank() }
                                            ?: "Нет описания",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${place.city ?: "—"} · ${place.address ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text(
                                        text = "Подробнее и фото →",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            placeVm.obtainEvent(PlaceIntent.OpenOwnerSchedule(place.id))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Расписание")
                                    }
                                    TextButton(
                                        onClick = {
                                            placeVm.obtainEvent(PlaceIntent.OpenVenueBookings(place.id))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Брони")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editTarget?.let { editing ->
        PlaceEditPlaceDialog(
            editTarget = editing,
            onDismiss = { editTarget = null },
            placeVm = placeVm,
            profile = profile,
            snackbarHostState = snackbarHostState
        )
    }

    PlaceVenueDetailBottomSheet(
        placeId = myVenueDetailPlaceId,
        placeVm = placeVm,
        onDismiss = { myVenueDetailPlaceId = null },
        showBookButton = true
    )

    when {
        placeState.venueBookingsPlace != null -> VenueBookingsDialog(placeVm = placeVm)
        placeState.ownerSchedulePlace != null -> OwnerScheduleDialog(placeVm = placeVm)
    }
}
