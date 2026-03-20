package com.app.quickfun.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.quickfun.domain.usecase.CheckAuthUseCase
import com.app.quickfun.domain.usecase.SignInUseCase
import com.app.quickfun.domain.usecase.SignUpUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val checkAuth: CheckAuthUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state

    init {
        checkUser()
    }

    private fun checkUser() {
        viewModelScope.launch {
            try {
                val isLogged = withContext(Dispatchers.IO) {
                    checkAuth()
                }

                _state.value = if (isLogged) {
                    AuthState.Authorized
                } else {
                    AuthState.Unauthorized
                }

            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading

            try {
                withContext(Dispatchers.IO) {
                    signInUseCase(email, password)
                }

                _state.value = AuthState.Authorized

            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Login error")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading

            try {
                val result = withContext(Dispatchers.IO) {
                    signUpUseCase(email, password)
                }

                _state.value = if (result.sessionActive) {
                    AuthState.Authorized
                } else {
                    AuthState.AwaitingEmailConfirmation(result.email)
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Register error")
            }
        }
    }

    fun dismissEmailConfirmation() {
        if (_state.value is AuthState.AwaitingEmailConfirmation) {
            _state.value = AuthState.Unauthorized
        }
    }

    fun toUnauthorizedAfterError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthorized
        }
    }
}