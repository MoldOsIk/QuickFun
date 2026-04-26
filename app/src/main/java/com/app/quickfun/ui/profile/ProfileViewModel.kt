package com.app.quickfun.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.quickfun.domain.usecase.GetProfileUseCase
import com.app.quickfun.domain.usecase.RegisterPlaceAsOwnerUseCase
import com.app.quickfun.domain.usecase.SaveProfileNameUseCase
import com.app.quickfun.ui.profile.model.ProfileEffect
import com.app.quickfun.ui.profile.model.ProfileIntent
import com.app.quickfun.ui.profile.model.ProfileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val saveProfileNameUseCase: SaveProfileNameUseCase,
    private val registerPlaceAsOwnerUseCase: RegisterPlaceAsOwnerUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state

    private val _effects = MutableSharedFlow<ProfileEffect>()
    val effects: SharedFlow<ProfileEffect> = _effects.asSharedFlow()

    init {
        obtainEvent(ProfileIntent.Refresh)
    }

    fun obtainEvent(event: ProfileIntent) {
        when (event) {
            ProfileIntent.Refresh -> refresh()
            is ProfileIntent.NameChanged -> onNameChange(event.value)
            ProfileIntent.SaveName -> saveName()
            is ProfileIntent.RegisterVenue -> registerVenue(event)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val profile = withContext(Dispatchers.IO) { getProfileUseCase() }
                _state.value = _state.value.copy(
                    isLoading = false,
                    userId = profile.id,
                    email = profile.email,
                    name = profile.name,
                    roles = profile.roles
                )
            } catch (e: Exception) {
                Log.e(TAG, "refresh failed", e)
                _state.value = _state.value.copy(isLoading = false)
                _effects.emit(ProfileEffect.ShowMessage("Не удалось загрузить профиль"))
            }
        }
    }

    private fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value)
    }

    private fun registerVenue(intent: ProfileIntent.RegisterVenue) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRegisteringVenue = true)
            try {
                withContext(Dispatchers.IO) {
                    registerPlaceAsOwnerUseCase(intent.draft)
                }
                _effects.emit(ProfileEffect.ShowMessage("Заявка на заведение отправлена на модерацию."))
                _effects.emit(ProfileEffect.VenueRegistered)
                obtainEvent(ProfileIntent.Refresh)
            } catch (e: IllegalArgumentException) {
                _effects.emit(ProfileEffect.ShowMessage(e.message ?: "Проверьте координаты и адрес."))
            } catch (e: Exception) {
                Log.e(TAG, "registerVenue failed", e)
                _effects.emit(ProfileEffect.ShowMessage("Не удалось отправить заявку. Попробуйте позже."))
            } finally {
                _state.value = _state.value.copy(isRegisteringVenue = false)
            }
        }
    }

    private fun saveName() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                withContext(Dispatchers.IO) {
                    saveProfileNameUseCase(_state.value.name)
                }
                _state.value = _state.value.copy(isSaving = false)
                _effects.emit(ProfileEffect.ShowMessage("Имя сохранено"))
                obtainEvent(ProfileIntent.Refresh)
            } catch (e: Exception) {
                Log.e(TAG, "saveName failed", e)
                _state.value = _state.value.copy(isSaving = false)
                _effects.emit(ProfileEffect.ShowMessage("Не удалось сохранить имя"))
            }
        }
    }
}
