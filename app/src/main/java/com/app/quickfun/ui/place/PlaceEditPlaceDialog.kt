package com.app.quickfun.ui.place

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.Place
import com.app.quickfun.ui.place.model.PlaceEffect
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.profile.model.ProfileState

@Composable
fun PlaceEditPlaceDialog(
    editTarget: Place,
    onDismiss: () -> Unit,
    placeVm: PlaceViewModel,
    profile: ProfileState,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val placeState by placeVm.state.collectAsState()
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editCity by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editCategoryId by remember { mutableStateOf("") }
    var editLatitude by remember { mutableStateOf("") }
    var editLongitude by remember { mutableStateOf("") }

    val editTargetLatest = rememberUpdatedState(editTarget)
    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val id = editTargetLatest.value.id
        if (uri != null) placeVm.obtainEvent(PlaceIntent.UploadVenueCover(id, uri))
    }
    val pickGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val id = editTargetLatest.value.id
        if (uri != null) placeVm.obtainEvent(PlaceIntent.AddVenueGalleryPhoto(id, uri))
    }

    val isSuperAdmin = !profile.isLoading &&
        profile.roles.any { it.equals("admin", ignoreCase = true) }

    fun canEdit(place: Place): Boolean {
        if (isSuperAdmin) return true
        val uid = profile.userId
        if (uid.isEmpty() || place.ownerId == null) return false
        return place.ownerId == uid
    }

    LaunchedEffect(placeVm, onDismiss) {
        placeVm.effects.collect { effect ->
            if (effect is PlaceEffect.EditPlaceSucceeded) {
                onDismiss()
            }
        }
    }

    LaunchedEffect(editTarget.id) {
        val p = editTarget
        editName = p.name
        editDescription = p.description.orEmpty()
        editCity = p.city.orEmpty()
        editAddress = p.address.orEmpty()
        editCategoryId = p.categoryId?.toString().orEmpty()
        editLatitude = p.latitude?.let { formatCoord(it) }.orEmpty()
        editLongitude = p.longitude?.let { formatCoord(it) }.orEmpty()
    }

    val editing = editTarget
    val mergedPlace = placeState.places.find { it.id == editing.id }
        ?: placeState.myPlaces.find { it.id == editing.id }
        ?: editing

    AlertDialog(
        onDismissRequest = {
            if (!placeState.isEditingPlace) onDismiss()
        },
        title = { Text("Редактировать место") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (canEdit(editing)) {
                    PlaceVenuePhotosEditor(
                        place = mergedPlace,
                        mediaBusy = placeState.isUploadingPlaceMedia || placeState.isEditingPlace,
                        onPickCover = {
                            pickCoverLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onPickGallery = {
                            pickGalleryLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onDeleteGalleryPhoto = { photoId ->
                            placeVm.obtainEvent(PlaceIntent.DeleteVenueGalleryPhoto(photoId))
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Название *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !placeState.isEditingPlace
                )
                OutlinedTextField(
                    value = editDescription,
                    onValueChange = { editDescription = it },
                    label = { Text("Описание") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    enabled = !placeState.isEditingPlace
                )
                OutlinedTextField(
                    value = editCity,
                    onValueChange = { editCity = it },
                    label = { Text("Город") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    enabled = !placeState.isEditingPlace
                )
                OutlinedTextField(
                    value = editAddress,
                    onValueChange = { editAddress = it },
                    label = { Text("Адрес") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    enabled = !placeState.isEditingPlace
                )
                OutlinedTextField(
                    value = editLatitude,
                    onValueChange = { editLatitude = it },
                    label = { Text("Широта (необяз.)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    enabled = !placeState.isEditingPlace,
                    placeholder = { Text("55.7558") }
                )
                OutlinedTextField(
                    value = editLongitude,
                    onValueChange = { editLongitude = it },
                    label = { Text("Долгота (необяз.)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    enabled = !placeState.isEditingPlace,
                    placeholder = { Text("37.6173") }
                )
                OutlinedTextField(
                    value = editCategoryId,
                    onValueChange = { editCategoryId = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ID категории (необяз.)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    enabled = !placeState.isEditingPlace
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val city = editCity.trim().takeIf { it.isNotEmpty() }
                    val address = editAddress.trim().takeIf { it.isNotEmpty() }
                    val err = venueFormCoordsAndAddressError(editLatitude, editLongitude, city, address)
                    if (err != null) {
                        scope.launch { snackbarHostState.showSnackbar(err) }
                        return@Button
                    }
                    val p = parseOptionalLatLonStrings(editLatitude, editLongitude)
                    val locId = editing.locationId ?: return@Button
                    val cat = editCategoryId.trim().toIntOrNull()
                    val latestCover = placeState.places.find { it.id == editing.id }
                        ?: placeState.myPlaces.find { it.id == editing.id }
                        ?: editing
                    placeVm.obtainEvent(
                        PlaceIntent.EditPlace(
                            placeId = editing.id,
                            locationId = locId,
                            name = editName,
                            description = editDescription.trim().takeIf { it.isNotEmpty() },
                            city = city,
                            address = address,
                            categoryId = cat,
                            latitude = p.latitude,
                            longitude = p.longitude,
                            status = editing.status ?: "approved",
                            coverImageUrl = latestCover.coverImageUrl
                        )
                    )
                },
                enabled = !placeState.isEditingPlace &&
                    !placeState.isUploadingPlaceMedia &&
                    editName.isNotBlank()
            ) {
                if (placeState.isEditingPlace) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text("Сохранить")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !placeState.isEditingPlace
            ) {
                Text("Отмена")
            }
        }
    )
}
