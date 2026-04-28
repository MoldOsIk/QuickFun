package com.app.quickfun.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.quickfun.domain.phone.PhoneE164
import com.app.quickfun.domain.usecase.CancelBookingUseCase
import com.app.quickfun.domain.usecase.GetMyActiveBookingsUseCase
import com.app.quickfun.domain.usecase.GetProfileUseCase
import com.app.quickfun.domain.usecase.RegisterPlaceAsOwnerUseCase
import com.app.quickfun.domain.usecase.SaveProfileNameUseCase
import com.app.quickfun.domain.usecase.SaveProfilePhoneUseCase
import com.app.quickfun.ui.common.dialCountryByIso
import com.app.quickfun.ui.common.parseStoredE164ToDraft
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
    private val saveProfilePhoneUseCase: SaveProfilePhoneUseCase,
    private val getMyActiveBookingsUseCase: GetMyActiveBookingsUseCase,
    private val cancelBookingUseCase: CancelBookingUseCase,
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
            is ProfileIntent.PhoneCountryIsoChanged -> onPhoneCountryIso(event.iso)
            is ProfileIntent.PhoneNationalDigitsChanged -> onPhoneNationalDigits(event.value)
            is ProfileIntent.PhoneE164ValidityChanged -> onPhoneE164Validity(event.e164)
            ProfileIntent.SavePhone -> savePhone()
            ProfileIntent.ClearPhone -> clearPhone()
            is ProfileIntent.CancelMyBooking -> cancelMyBooking(event.bookingId)
            is ProfileIntent.RegisterVenue -> registerVenue(event)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val profile = withContext(Dispatchers.IO) { getProfileUseCase() }
                val (country, national) = parseStoredE164ToDraft(profile.phoneE164)
                val valid = PhoneE164.validE164IfComplete(
                    country.dialDigits,
                    national,
                    country.nationalDigits
                )
                val bookings = try {
                    withContext(Dispatchers.IO) { getMyActiveBookingsUseCase() }
                } catch (e: Exception) {
                    Log.e(TAG, "load my bookings failed", e)
                    emptyList()
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    userId = profile.id,
                    email = profile.email,
                    name = profile.name,
                    phoneCountryIso = country.iso,
                    phoneNationalDigits = national,
                    phoneE164Valid = valid,
                    roles = profile.roles,
                    myActiveBookings = bookings,
                    cancellingMyBookingId = null
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

    private fun onPhoneCountryIso(iso: String) {
        val c = dialCountryByIso(iso)
        val national = PhoneE164.digitsOnly(_state.value.phoneNationalDigits).take(c.nationalDigits)
        _state.value = _state.value.copy(
            phoneCountryIso = c.iso,
            phoneNationalDigits = national,
            phoneE164Valid = PhoneE164.validE164IfComplete(c.dialDigits, national, c.nationalDigits)
        )
    }

    private fun onPhoneNationalDigits(value: String) {
        val c = dialCountryByIso(_state.value.phoneCountryIso)
        val national = PhoneE164.digitsOnly(value).take(c.nationalDigits)
        _state.value = _state.value.copy(
            phoneNationalDigits = national,
            phoneE164Valid = PhoneE164.validE164IfComplete(c.dialDigits, national, c.nationalDigits)
        )
    }

    private fun onPhoneE164Validity(e164: String?) {
        _state.value = _state.value.copy(phoneE164Valid = e164)
    }

    private fun savePhone() {
        viewModelScope.launch {
            val e164 = _state.value.phoneE164Valid
            if (e164 == null) {
                _effects.emit(ProfileEffect.ShowMessage("Введите номер полностью или очистите поле."))
                return@launch
            }
            _state.value = _state.value.copy(isSavingPhone = true)
            try {
                withContext(Dispatchers.IO) { saveProfilePhoneUseCase(e164) }
                _state.value = _state.value.copy(isSavingPhone = false)
                _effects.emit(ProfileEffect.ShowMessage("Телефон сохранён"))
                obtainEvent(ProfileIntent.Refresh)
            } catch (e: Exception) {
                Log.e(TAG, "savePhone failed", e)
                _state.value = _state.value.copy(isSavingPhone = false)
                _effects.emit(ProfileEffect.ShowMessage("Не удалось сохранить телефон"))
            }
        }
    }

    private fun clearPhone() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingPhone = true)
            try {
                withContext(Dispatchers.IO) { saveProfilePhoneUseCase(null) }
                _state.value = _state.value.copy(
                    isSavingPhone = false,
                    phoneCountryIso = "RU",
                    phoneNationalDigits = "",
                    phoneE164Valid = null
                )
                _effects.emit(ProfileEffect.ShowMessage("Телефон удалён из профиля"))
                obtainEvent(ProfileIntent.Refresh)
            } catch (e: Exception) {
                Log.e(TAG, "clearPhone failed", e)
                _state.value = _state.value.copy(isSavingPhone = false)
                _effects.emit(ProfileEffect.ShowMessage("Не удалось удалить телефон"))
            }
        }
    }

    private fun cancelMyBooking(bookingId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(cancellingMyBookingId = bookingId)
            try {
                withContext(Dispatchers.IO) { cancelBookingUseCase(bookingId) }
                val bookings = withContext(Dispatchers.IO) { getMyActiveBookingsUseCase() }
                _state.value = _state.value.copy(
                    myActiveBookings = bookings,
                    cancellingMyBookingId = null
                )
                _effects.emit(ProfileEffect.ShowMessage("Бронь отменена."))
            } catch (e: Exception) {
                Log.e(TAG, "cancelMyBooking failed", e)
                _state.value = _state.value.copy(cancellingMyBookingId = null)
                _effects.emit(ProfileEffect.ShowMessage("Не удалось отменить бронь."))
            }
        }
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
