package com.app.quickfun.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.BookOnline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.quickfun.di.AppModule
import com.app.quickfun.domain.model.MyActiveBooking
import com.app.quickfun.domain.model.PlaceRegistrationDraft
import com.app.quickfun.domain.model.VenueCategory
import com.app.quickfun.ui.common.PhoneE164Field
import com.app.quickfun.ui.common.dialCountryByIso
import com.app.quickfun.ui.place.VenueFormFields
import com.app.quickfun.ui.place.formatBookingSlotRange
import com.app.quickfun.ui.place.parseOptionalLatLonStrings
import com.app.quickfun.ui.place.venueFormCoordsAndAddressError
import com.app.quickfun.ui.profile.model.ProfileEffect
import com.app.quickfun.ui.profile.model.ProfileIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    profileVm: ProfileViewModel,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit
) {
    val profile by profileVm.state.collectAsState()
    var showSignOutButton by remember { mutableStateOf(false) }
    LaunchedEffect(profile.isLoading) {
        if (profile.isLoading) {
            showSignOutButton = false
        } else {
            delay(300)
            showSignOutButton = true
        }
    }
    var showVenueDialog by remember { mutableStateOf(false) }
    var vName by remember { mutableStateOf("") }
    var vDesc by remember { mutableStateOf("") }
    var vCity by remember { mutableStateOf("") }
    var vAddress by remember { mutableStateOf("") }
    var vLat by remember { mutableStateOf("") }
    var vLng by remember { mutableStateOf("") }
    var vCategoryId by remember { mutableStateOf<Int?>(null) }
    var venueCategories by remember { mutableStateOf<List<VenueCategory>>(emptyList()) }
    var venueCategoriesLoading by remember { mutableStateOf(false) }
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
            vCategoryId = null
        } else if (venueCategories.isEmpty() && !venueCategoriesLoading) {
            venueCategoriesLoading = true
            venueCategories = runCatching {
                withContext(Dispatchers.IO) { AppModule.getVenueCategoriesUseCase() }
            }.getOrElse { emptyList() }
            venueCategoriesLoading = false
        }
    }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surface
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        AnimatedContent(
            targetState = profile.isLoading,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280)) + slideInVertically { it / 5 })
                    .togetherWith(fadeOut(tween(180)) + slideOutVertically { -it / 8 })
            },
            label = "profile_loading_switch"
        ) { loading ->
            if (loading) {
                ProfileLoadingPlaceholder()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    ProfileHeroHeader(
                        name = profile.name,
                        email = profile.email,
                        roles = profile.roles
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val canRegisterVenueFromProfile = profile.roles.any {
                            it.equals("place_admin", ignoreCase = true)
                        }

                        ProfileSectionCard(
                            title = "Личные данные",
                            icon = Icons.Outlined.Edit,
                            delayIndex = 0
                        ) {
                            OutlinedTextField(
                                value = profile.name,
                                onValueChange = { profileVm.obtainEvent(ProfileIntent.NameChanged(it)) },
                                label = { Text("Имя") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Outlined.Person, contentDescription = null)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { profileVm.obtainEvent(ProfileIntent.SaveName) },
                                enabled = !profile.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (profile.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Сохранить имя")
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Телефон для связи по брони",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val phoneCountry = dialCountryByIso(profile.phoneCountryIso)
                            PhoneE164Field(
                                selectedCountry = phoneCountry,
                                onCountryChange = { c ->
                                    profileVm.obtainEvent(ProfileIntent.PhoneCountryIsoChanged(c.iso))
                                },
                                nationalDigits = profile.phoneNationalDigits,
                                onNationalDigitsChange = {
                                    profileVm.obtainEvent(ProfileIntent.PhoneNationalDigitsChanged(it))
                                },
                                onE164Change = {
                                    profileVm.obtainEvent(ProfileIntent.PhoneE164ValidityChanged(it))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !profile.isSavingPhone,
                                label = { Text("Номер (необязательно)") }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { profileVm.obtainEvent(ProfileIntent.SavePhone) },
                                    enabled = !profile.isSavingPhone,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (profile.isSavingPhone) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text("Сохранить")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { profileVm.obtainEvent(ProfileIntent.ClearPhone) },
                                    enabled = !profile.isSavingPhone,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Убрать")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Телефон виден владельцу площадки по вашим броням.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ProfileSectionCard(
                            title = "Мои брони",
                            icon = Icons.Outlined.BookOnline,
                            delayIndex = 1
                        ) {
                            if (profile.myActiveBookings.isEmpty()) {
                                EmptyBookingsHint()
                            } else {
                                profile.myActiveBookings.forEachIndexed { index, b ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    BookingRowCard(
                                        booking = b,
                                        isCancelling = profile.cancellingMyBookingId == b.bookingId,
                                        onCancel = {
                                            profileVm.obtainEvent(ProfileIntent.CancelMyBooking(b.bookingId))
                                        }
                                    )
                                }
                            }
                        }

                        if (canRegisterVenueFromProfile) {
                            ProfileSectionCard(
                                title = "Заведение",
                                icon = Icons.Outlined.AddBusiness,
                                delayIndex = 2
                            ) {
                                Text(
                                    text = "Подайте заявку на новое место — модератор рассмотрит её в каталоге.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showVenueDialog = true },
                                    enabled = !profile.isRegisteringVenue,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.AddBusiness,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Добавить заведение")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AnimatedVisibility(
                            visible = showSignOutButton,
                            enter = fadeIn(tween(400)) + slideInVertically { it / 8 }
                        ) {
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Выйти из аккаунта")
                            }
                        }
                    }
                }
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
                    venueCategories = venueCategories,
                    selectedCategoryId = vCategoryId,
                    onSelectedCategoryIdChange = { vCategoryId = it },
                    venueCategoriesLoading = venueCategoriesLoading,
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
                            categoryId = vCategoryId,
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

@Composable
private fun ProfileLoadingPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Загружаем профиль…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileHeroHeader(
    name: String,
    email: String,
    roles: List<String>
) {
    val scheme = MaterialTheme.colorScheme
    val initials = remember(name, email) { profileInitials(name, email) }
    val headerOn = scheme.onPrimaryContainer
    val headerMuted = scheme.onPrimaryContainer.copy(alpha = 0.78f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
    ) {
        // Спокойный фон: контейнер + лёгкий акцент primary сверху (без смешения secondary/tertiary)
        Box(modifier = Modifier.fillMaxSize().background(scheme.primaryContainer))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.26f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Декоративные «пузыри»
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-24).dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 40.dp)
                .size(90.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.08f))
        )
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(96.dp),
            tint = scheme.primary.copy(alpha = 0.14f)
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = scheme.surface.copy(alpha = 0.98f),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greetingName(name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = headerOn,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = headerMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = headerMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (roles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roles.forEach { role ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = scheme.surface.copy(alpha = 0.92f),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = scheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = roleLabelRu(role),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = scheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Подписи ролей с бэкенда (snake_case) — всегда по-русски в UI. */
private fun roleLabelRu(roleRaw: String): String {
    val key = roleRaw.trim().lowercase()
    return when (key) {
        "admin" -> "Администратор"
        "user" -> "Пользователь"
        "place_admin" -> "Владелец заведений"
        else -> "Прочая роль"
    }
}

private fun profileInitials(name: String, email: String): String {
    val n = name.trim()
    if (n.length >= 2) {
        return n.take(2).uppercase()
    }
    if (n.isNotEmpty()) {
        return n.take(1).uppercase()
    }
    val local = email.substringBefore('@').trim()
    if (local.length >= 2) return local.take(2).uppercase()
    return if (local.isNotEmpty()) local.take(1).uppercase() else "?"
}

private fun greetingName(name: String): String {
    val n = name.trim()
    return if (n.isNotEmpty()) "Привет, $n!" else "Ваш профиль"
}

@Composable
private fun ProfileSectionCard(
    title: String,
    icon: ImageVector,
    delayIndex: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(delayIndex) {
        delay((delayIndex * 70 + 40).toLong())
        visible = true
    }
    val lift by animateDpAsState(
        targetValue = if (visible) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_lift"
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320, delayMillis = delayIndex * 60)) +
            expandVertically(tween(320, delayMillis = delayIndex * 60)),
        exit = fadeOut() + shrinkVertically()
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = lift)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun EmptyBookingsHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.BookOnline,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Пока нет активных броней",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Забронируйте место в каталоге или на карте — список обновится здесь.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BookingRowCard(
    booking: MyActiveBooking,
    isCancelling: Boolean,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = booking.placeName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatBookingSlotRange(booking.startTimeIso, booking.endTimeIso),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = booking.displaySeat(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onCancel,
                enabled = !isCancelling,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Отменить бронь")
                }
            }
        }
    }
}
