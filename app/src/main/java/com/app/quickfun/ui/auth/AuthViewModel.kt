package com.app.quickfun.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.quickfun.domain.usecase.CheckAuthUseCase
import com.app.quickfun.domain.usecase.RegisterPlaceAsOwnerUseCase
import com.app.quickfun.domain.usecase.SaveProfileNameUseCase
import com.app.quickfun.domain.usecase.SaveProfilePhoneUseCase
import com.app.quickfun.domain.usecase.SignInUseCase
import com.app.quickfun.domain.usecase.SignOutUseCase
import com.app.quickfun.domain.usecase.SignUpUseCase
import com.app.quickfun.ui.auth.model.AuthEffect
import com.app.quickfun.ui.auth.model.AuthIntent
import com.app.quickfun.ui.auth.model.AuthState
import com.app.quickfun.ui.auth.model.RegistrationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val checkAuth: CheckAuthUseCase,
    private val saveProfileNameUseCase: SaveProfileNameUseCase,
    private val saveProfilePhoneUseCase: SaveProfilePhoneUseCase,
    private val registerPlaceAsOwnerUseCase: RegisterPlaceAsOwnerUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state
    private val _effects = MutableSharedFlow<AuthEffect>()
    val effects: SharedFlow<AuthEffect> = _effects.asSharedFlow()

    init {
        obtainEvent(AuthIntent.CheckSession)
    }

    /** События экрана (MVI), см. [com.app.quickfun.ui.auth.model.AuthIntent]. */
    fun obtainEvent(event: AuthIntent) {
        when (event) {
            AuthIntent.CheckSession -> checkUser()
            is AuthIntent.SignIn -> signIn(event.email, event.password)
            is AuthIntent.SignUp -> signUp(
                event.email,
                event.password,
                event.displayName,
                event.mode,
                event.phoneE164
            )
            AuthIntent.DismissEmailConfirmation -> dismissEmailConfirmation()
            AuthIntent.BackToUnauthorized -> _state.value = AuthState.Unauthorized
            AuthIntent.SignOut -> signOut()
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { signOutUseCase() }
            } catch (e: Exception) {
                Log.e(TAG, "signOut failed", e)
            } finally {
                _state.value = AuthState.Unauthorized
            }
        }
    }

    private fun checkUser() {
        viewModelScope.launch {
            try {
                val isLogged = withContext(Dispatchers.IO) { checkAuth() }
                _state.value = if (isLogged) {
                    AuthState.Authorized
                } else {
                    AuthState.Unauthorized
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkUser failed", e)
                _state.value = AuthState.Unauthorized
            }
        }
    }

    private fun signIn(email: String, password: String) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                _effects.emit(AuthEffect.ShowMessage("Введите email и пароль."))
                _state.value = AuthState.Unauthorized
                return@launch
            }

            _state.value = AuthState.Loading
            try {
                withContext(Dispatchers.IO) { signInUseCase(email, password) }
                _state.value = AuthState.Authorized
            } catch (e: Exception) {
                Log.e(TAG, "signIn failed", e)
                _state.value = AuthState.Unauthorized
                _effects.emit(AuthEffect.ShowMessage(mapSignInError(e)))
            }
        }
    }

    private fun signUp(
        email: String,
        password: String,
        displayName: String,
        mode: RegistrationMode,
        phoneE164: String?
    ) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                _effects.emit(AuthEffect.ShowMessage("Введите email и пароль."))
                _state.value = AuthState.Unauthorized
                return@launch
            }
            if (password.length < 6) {
                _effects.emit(AuthEffect.ShowMessage("Пароль должен быть не короче 6 символов."))
                _state.value = AuthState.Unauthorized
                return@launch
            }

            _state.value = AuthState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    signUpUseCase(
                        email,
                        password,
                        displayName.ifBlank { null },
                        phoneE164?.trim()?.takeIf { it.isNotEmpty() }
                    )
                }
                if (result.sessionActive) {
                    val name = displayName.trim()
                    if (name.isNotEmpty()) {
                        withContext(Dispatchers.IO) { saveProfileNameUseCase(name) }
                    }
                    val phone = phoneE164?.trim()?.takeIf { it.isNotEmpty() }
                    if (phone != null) {
                        try {
                            withContext(Dispatchers.IO) { saveProfilePhoneUseCase(phone) }
                        } catch (e: Exception) {
                            Log.e(TAG, "saveProfilePhone after signUp failed", e)
                        }
                    }
                    if (mode is RegistrationMode.PlaceOwner) {
                        try {
                            withContext(Dispatchers.IO) {
                                registerPlaceAsOwnerUseCase(mode.draft)
                            }
                            _effects.emit(
                                AuthEffect.ShowMessage(
                                    "Заявка на заведение отправлена на модерацию."
                                )
                            )
                        } catch (e: IllegalArgumentException) {
                            _effects.emit(
                                AuthEffect.ShowMessage(
                                    e.message ?: "Проверьте координаты и адрес заведения."
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "registerPlaceAsOwner failed", e)
                            _effects.emit(
                                AuthEffect.ShowMessage(
                                    "Аккаунт создан, но не удалось зарегистрировать заведение. " +
                                        "Добавьте его в профиле."
                                )
                            )
                        }
                    }
                    _state.value = AuthState.Authorized
                } else {
                    if (mode is RegistrationMode.PlaceOwner) {
                        _effects.emit(
                            AuthEffect.ShowMessage(
                                "Подтвердите email, затем войдите и в профиле нажмите «Добавить заведение»."
                            )
                        )
                    }
                    _state.value = AuthState.AwaitingEmailConfirmation(result.email)
                }
            } catch (e: Exception) {
                Log.e(TAG, "signUp failed", e)
                _state.value = AuthState.Unauthorized
                _effects.emit(AuthEffect.ShowMessage(mapSignUpError(e)))
            }
        }
    }

    private fun dismissEmailConfirmation() {
        if (_state.value is AuthState.AwaitingEmailConfirmation) {
            _state.value = AuthState.Unauthorized
        }
    }

    private fun mapSignInError(error: Exception): String {
        val message = error.message?.lowercase().orEmpty()
        return when {
            "invalid login credentials" in message -> "Неверный email или пароль."
            "email not confirmed" in message -> "Подтвердите email в письме, затем войдите."
            "user not found" in message -> "Аккаунт не найден."
            else -> "Не удалось войти. Проверьте данные и попробуйте снова."
        }
    }

    private fun mapSignUpError(error: Exception): String {
        val message = error.message?.lowercase().orEmpty()
        return when {
            "user already registered" in message -> "Такой email уже зарегистрирован."
            "password should be at least" in message -> "Слишком короткий пароль."
            else -> "Не удалось зарегистрироваться. Проверьте данные."
        }
    }
}
