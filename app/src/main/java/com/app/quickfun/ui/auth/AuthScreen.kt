package com.app.quickfun.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.PlaceRegistrationDraft
import com.app.quickfun.ui.auth.model.AuthEffect
import com.app.quickfun.ui.auth.model.AuthIntent
import com.app.quickfun.ui.auth.model.AuthState
import com.app.quickfun.ui.auth.model.RegistrationMode
import com.app.quickfun.ui.place.VenueFormFields
import com.app.quickfun.ui.place.parseOptionalLatLonStrings
import com.app.quickfun.ui.place.venueFormCoordsAndAddressError
import kotlinx.coroutines.launch

private enum class AuthEntryRoute {
    Login,
    Register
}

@Composable
fun AuthScreen(vm: AuthViewModel) {
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var registerAsPlaceOwner by remember { mutableStateOf(false) }
    var vPlaceName by remember { mutableStateOf("") }
    var vDesc by remember { mutableStateOf("") }
    var vCity by remember { mutableStateOf("") }
    var vAddress by remember { mutableStateOf("") }
    var vLat by remember { mutableStateOf("") }
    var vLng by remember { mutableStateOf("") }
    var vCat by remember { mutableStateOf("") }
    var route by remember { mutableStateOf(AuthEntryRoute.Login) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.surface
        )
    )

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is AuthEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(paddingValues)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is AuthState.Loading -> CircularProgressIndicator()
                is AuthState.Unauthorized -> {
                    when (route) {
                        AuthEntryRoute.Login -> LoginForm(
                            email = email,
                            password = password,
                            onEmailChange = { email = it },
                            onPasswordChange = { password = it },
                            onSignIn = { vm.obtainEvent(AuthIntent.SignIn(email, password)) },
                            onGoToRegister = { route = AuthEntryRoute.Register }
                        )
                        AuthEntryRoute.Register -> RegisterForm(
                            email = email,
                            password = password,
                            displayName = displayName,
                            registerAsPlaceOwner = registerAsPlaceOwner,
                            onRegisterAsPlaceOwnerChange = { registerAsPlaceOwner = it },
                            vPlaceName = vPlaceName,
                            onVPlaceNameChange = { vPlaceName = it },
                            vDesc = vDesc,
                            onVDescChange = { vDesc = it },
                            vCity = vCity,
                            onVCityChange = { vCity = it },
                            vAddress = vAddress,
                            onVAddressChange = { vAddress = it },
                            vLat = vLat,
                            onVLatChange = { vLat = it },
                            vLng = vLng,
                            onVLngChange = { vLng = it },
                            vCat = vCat,
                            onVCatChange = { vCat = it },
                            onEmailChange = { email = it },
                            onPasswordChange = { password = it },
                            onDisplayNameChange = { displayName = it },
                            onSignUp = {
                                scope.launch {
                                    val mode = if (registerAsPlaceOwner) {
                                        if (vPlaceName.isBlank()) {
                                            snackbarHostState.showSnackbar("Укажите название заведения.")
                                            return@launch
                                        }
                                        val city = vCity.trim().takeIf { it.isNotEmpty() }
                                        val address = vAddress.trim().takeIf { it.isNotEmpty() }
                                        val geoErr = venueFormCoordsAndAddressError(vLat, vLng, city, address)
                                        if (geoErr != null) {
                                            snackbarHostState.showSnackbar(geoErr)
                                            return@launch
                                        }
                                        val p = parseOptionalLatLonStrings(vLat, vLng)
                                        RegistrationMode.PlaceOwner(
                                            PlaceRegistrationDraft(
                                                name = vPlaceName.trim(),
                                                description = vDesc.trim().takeIf { it.isNotEmpty() },
                                                city = city,
                                                address = address,
                                                categoryId = vCat.trim().toIntOrNull(),
                                                latitude = p.latitude,
                                                longitude = p.longitude
                                            )
                                        )
                                    } else {
                                        RegistrationMode.User
                                    }
                                    vm.obtainEvent(AuthIntent.SignUp(email, password, displayName, mode))
                                }
                            },
                            onGoToLogin = { route = AuthEntryRoute.Login }
                        )
                    }
                }
                is AuthState.AwaitingEmailConfirmation -> AwaitingConfirmation(
                    email = (state as AuthState.AwaitingEmailConfirmation).email,
                    onBack = {
                        vm.obtainEvent(AuthIntent.DismissEmailConfirmation)
                        route = AuthEntryRoute.Login
                    }
                )
                is AuthState.Error -> ErrorCard(
                    message = (state as AuthState.Error).message,
                    onBack = { vm.obtainEvent(AuthIntent.BackToUnauthorized) }
                )
                is AuthState.Authorized -> Unit
            }
        }
    }
}

@Composable
private fun LoginForm(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onGoToRegister: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Вход",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Войдите по email и паролю",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Войти")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onGoToRegister) {
                Text("Нет аккаунта? Зарегистрироваться")
            }
        }
    }
}

@Composable
private fun RegisterForm(
    email: String,
    password: String,
    displayName: String,
    registerAsPlaceOwner: Boolean,
    onRegisterAsPlaceOwnerChange: (Boolean) -> Unit,
    vPlaceName: String,
    onVPlaceNameChange: (String) -> Unit,
    vDesc: String,
    onVDescChange: (String) -> Unit,
    vCity: String,
    onVCityChange: (String) -> Unit,
    vAddress: String,
    onVAddressChange: (String) -> Unit,
    vLat: String,
    onVLatChange: (String) -> Unit,
    vLng: String,
    onVLngChange: (String) -> Unit,
    vCat: String,
    onVCatChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSignUp: () -> Unit,
    onGoToLogin: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Регистрация",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Пользователь или владелец нового заведения",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Имя") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = registerAsPlaceOwner,
                    onCheckedChange = onRegisterAsPlaceOwnerChange
                )
                Text(
                    text = "Регистрируюсь как владелец заведения (заявка на модерацию)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (registerAsPlaceOwner) {
                Spacer(modifier = Modifier.height(12.dp))
                VenueFormFields(
                    placeName = vPlaceName,
                    onPlaceNameChange = onVPlaceNameChange,
                    description = vDesc,
                    onDescriptionChange = onVDescChange,
                    city = vCity,
                    onCityChange = onVCityChange,
                    address = vAddress,
                    onAddressChange = onVAddressChange,
                    latitude = vLat,
                    onLatitudeChange = onVLatChange,
                    longitude = vLng,
                    onLongitudeChange = onVLngChange,
                    categoryId = vCat,
                    onCategoryIdChange = onVCatChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Зарегистрироваться")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onGoToLogin) {
                Text("Уже есть аккаунт? Войти")
            }
        }
    }
}

@Composable
private fun AwaitingConfirmation(
    email: String,
    onBack: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Подтвердите email",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Мы отправили письмо на:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = email, fontWeight = FontWeight.Medium)
                Text(
                    text = "После подтверждения вернитесь и выполните вход.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("К экрану входа")
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onBack: () -> Unit
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Что-то пошло не так",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) {
                Text("Назад")
            }
        }
    }
}
