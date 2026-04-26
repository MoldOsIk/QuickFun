package com.app.quickfun.ui.place

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import com.app.quickfun.domain.model.Place
import com.app.quickfun.ui.place.model.PlaceEffect
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.profile.model.ProfileState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceCatalogTab(
    placeVm: PlaceViewModel,
    profile: ProfileState,
    snackbarHostState: SnackbarHostState
) {
    val placeState by placeVm.state.collectAsState()
    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var addName by remember { mutableStateOf("") }
    var addDescription by remember { mutableStateOf("") }
    var addCity by remember { mutableStateOf("") }
    var addAddress by remember { mutableStateOf("") }
    var addCategoryId by remember { mutableStateOf("") }
    var addLatitude by remember { mutableStateOf("") }
    var addLongitude by remember { mutableStateOf("") }

    var editTarget by remember { mutableStateOf<Place?>(null) }
    var catalogDetailPlaceId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isSuperAdmin = !profile.isLoading &&
        profile.roles.any { it.equals("admin", ignoreCase = true) }

    fun canEdit(place: Place): Boolean {
        if (isSuperAdmin) return true
        val uid = profile.userId
        if (uid.isEmpty() || place.ownerId == null) return false
        return place.ownerId == uid
    }

    LaunchedEffect(placeVm) {
        placeVm.effects.collect { effect ->
            when (effect) {
                PlaceEffect.AddPlaceSucceeded -> {
                    showAddPlaceDialog = false
                    addName = ""
                    addDescription = ""
                    addCity = ""
                    addAddress = ""
                    addCategoryId = ""
                    addLatitude = ""
                    addLongitude = ""
                }
                PlaceEffect.EditPlaceSucceeded -> Unit
                is PlaceEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSuperAdmin) {
            TextButton(
                onClick = { showAddPlaceDialog = true },
                enabled = !placeState.isAddingPlace && !placeState.isEditingPlace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("+ Добавить место (как администратор)")
            }
        }

        when {
            placeState.isLoading && placeState.places.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    items(placeState.places, key = { it.id }) { place ->
                        val approved = place.status?.lowercase() == "approved"
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
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { catalogDetailPlaceId = place.id }
                                ) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    PlaceVenueCoverOnly(place = place, height = 120.dp)
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
                                        text = "Категория: ${place.categoryName ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text(
                                        text = "${place.city ?: "—"}, ${place.address ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = run {
                                            val lat = place.latitude
                                            val lon = place.longitude
                                            if (lat != null && lon != null) {
                                                "${formatCoord(lat)}, ${formatCoord(lon)}"
                                            } else "—"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = "Подробнее и фото →",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                if (approved) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            placeVm.obtainEvent(PlaceIntent.LoadBookableSlots(place.id))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Забронировать")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPlaceDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!placeState.isAddingPlace && !placeState.isEditingPlace) {
                    showAddPlaceDialog = false
                }
            },
            title = { Text("Новое место") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Название *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                    )
                    OutlinedTextField(
                        value = addDescription,
                        onValueChange = { addDescription = it },
                        label = { Text("Описание") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                    )
                    OutlinedTextField(
                        value = addCity,
                        onValueChange = { addCity = it },
                        label = { Text("Город") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                    )
                    OutlinedTextField(
                        value = addAddress,
                        onValueChange = { addAddress = it },
                        label = { Text("Адрес") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                    )
                    OutlinedTextField(
                        value = addLatitude,
                        onValueChange = { addLatitude = it },
                        label = { Text("Широта (необяз.)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace,
                        placeholder = { Text("55.7558") }
                    )
                    OutlinedTextField(
                        value = addLongitude,
                        onValueChange = { addLongitude = it },
                        label = { Text("Долгота (необяз.)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace,
                        placeholder = { Text("37.6173") }
                    )
                    OutlinedTextField(
                        value = addCategoryId,
                        onValueChange = { addCategoryId = it.filter { ch -> ch.isDigit() } },
                        label = { Text("ID категории (необяз.)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val city = addCity.trim().takeIf { it.isNotEmpty() }
                        val address = addAddress.trim().takeIf { it.isNotEmpty() }
                        val err = venueFormCoordsAndAddressError(addLatitude, addLongitude, city, address)
                        if (err != null) {
                            scope.launch { snackbarHostState.showSnackbar(err) }
                            return@Button
                        }
                        val p = parseOptionalLatLonStrings(addLatitude, addLongitude)
                        val cat = addCategoryId.trim().toIntOrNull()
                        placeVm.obtainEvent(
                            PlaceIntent.AddPlace(
                                name = addName,
                                description = addDescription.trim().takeIf { it.isNotEmpty() },
                                city = city,
                                address = address,
                                categoryId = cat,
                                latitude = p.latitude,
                                longitude = p.longitude
                            )
                        )
                    },
                    enabled = !placeState.isAddingPlace && !placeState.isEditingPlace && addName.isNotBlank()
                ) {
                    if (placeState.isAddingPlace) {
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                    } else {
                        Text("Сохранить")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddPlaceDialog = false },
                    enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                ) {
                    Text("Отмена")
                }
            }
        )
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
        placeId = catalogDetailPlaceId,
        placeVm = placeVm,
        onDismiss = { catalogDetailPlaceId = null },
        showBookButton = true
    )
}
