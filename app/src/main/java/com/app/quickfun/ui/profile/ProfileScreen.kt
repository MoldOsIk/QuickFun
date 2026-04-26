package com.app.quickfun.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.PlaceRegistrationDraft
import com.app.quickfun.ui.place.VenueFormFields
import com.app.quickfun.ui.profile.model.ProfileEffect
import com.app.quickfun.ui.profile.model.ProfileIntent
import com.app.quickfun.ui.place.parseOptionalLatLonStrings
import com.app.quickfun.ui.place.venueFormCoordsAndAddressError

@Composable
fun ProfileScreen(
    profileVm: ProfileViewModel,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit
) {
    val profile by profileVm.state.collectAsState()
    var showVenueDialog by remember { mutableStateOf(false) }
    var vName by remember { mutableStateOf("") }
    var vDesc by remember { mutableStateOf("") }
    var vCity by remember { mutableStateOf("") }
    var vAddress by remember { mutableStateOf("") }
    var vLat by remember { mutableStateOf("") }
    var vLng by remember { mutableStateOf("") }
    var vCat by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profileVm) {
        profileVm.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                ProfileEffect.VenueRegistered -> showVenueDialog = false
            }
        }
    }

    LaunchedEffect(showVenueDialog) {
        if (!showVenueDialog) {
            vName = ""
            vDesc = ""
            vCity = ""
            vAddress = ""
            vLat = ""
            vLng = ""
            vCat = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Профиль",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (profile.isLoading) {
            CircularProgressIndicator()
        } else {
            val canRegisterVenueFromProfile = profile.roles.any {
                it.equals("place_admin", ignoreCase = true)
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = profile.email,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (profile.roles.isEmpty()) "Роли не назначены"
                        else profile.roles.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = profile.name,
                        onValueChange = { profileVm.obtainEvent(ProfileIntent.NameChanged(it)) },
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { profileVm.obtainEvent(ProfileIntent.SaveName) },
                        enabled = !profile.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (profile.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        } else {
                            Text("Сохранить имя")
                        }
                    }
                }
            }

            if (canRegisterVenueFromProfile) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showVenueDialog = true },
                    enabled = !profile.isRegisteringVenue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Добавить заведение (заявка на модерацию)")
                }
                Text(
                    text = "Отправьте заявку на ещё одно заведение — главный администратор её рассмотрит.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выйти")
            }
        }
    }

    if (showVenueDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!profile.isRegisteringVenue) showVenueDialog = false
            },
            title = { Text("Заявка на заведение") },
            text = {
                VenueFormFields(
                    placeName = vName,
                    onPlaceNameChange = { vName = it },
                    description = vDesc,
                    onDescriptionChange = { vDesc = it },
                    city = vCity,
                    onCityChange = { vCity = it },
                    address = vAddress,
                    onAddressChange = { vAddress = it },
                    latitude = vLat,
                    onLatitudeChange = { vLat = it },
                    longitude = vLng,
                    onLongitudeChange = { vLng = it },
                    categoryId = vCat,
                    onCategoryIdChange = { vCat = it.filter { ch -> ch.isDigit() } },
                    enabled = !profile.isRegisteringVenue
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vName.isBlank()) return@Button
                        val city = vCity.trim().takeIf { it.isNotEmpty() }
                        val address = vAddress.trim().takeIf { it.isNotEmpty() }
                        val err = venueFormCoordsAndAddressError(vLat, vLng, city, address)
                        if (err != null) {
                            scope.launch { snackbarHostState.showSnackbar(err) }
                            return@Button
                        }
                        val p = parseOptionalLatLonStrings(vLat, vLng)
                        val draft = PlaceRegistrationDraft(
                            name = vName.trim(),
                            description = vDesc.trim().takeIf { it.isNotEmpty() },
                            city = city,
                            address = address,
                            categoryId = vCat.trim().toIntOrNull(),
                            latitude = p.latitude,
                            longitude = p.longitude
                        )
                        profileVm.obtainEvent(ProfileIntent.RegisterVenue(draft))
                    },
                    enabled = !profile.isRegisteringVenue && vName.isNotBlank()
                ) {
                    if (profile.isRegisteringVenue) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    } else {
                        Text("Отправить")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showVenueDialog = false },
                    enabled = !profile.isRegisteringVenue
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}
