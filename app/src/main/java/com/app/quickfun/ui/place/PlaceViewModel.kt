package com.app.quickfun.ui.place

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.quickfun.domain.model.VenueKind
import com.app.quickfun.domain.model.resolveVenueKind
import com.app.quickfun.domain.usecase.AddCinemaSessionUseCase
import com.app.quickfun.domain.usecase.AddSeatUseCase
import com.app.quickfun.domain.usecase.GenerateVenueSeatLayoutUseCase
import com.app.quickfun.domain.usecase.ListSeatsUseCase
import com.app.quickfun.domain.usecase.ApprovePlaceUseCase
import com.app.quickfun.domain.usecase.CreateBookingUseCase
import com.app.quickfun.domain.usecase.CreatePlaceUseCase
import com.app.quickfun.domain.usecase.CreateWeekTimeSlotsUseCase
import com.app.quickfun.domain.usecase.GetAvailableBookableSlotsUseCase
import com.app.quickfun.domain.usecase.GetMyPlacesUseCase
import com.app.quickfun.domain.usecase.GetPendingPlacesUseCase
import com.app.quickfun.domain.usecase.GetPlacesUseCase
import com.app.quickfun.domain.usecase.LoadOwnerBookingScheduleUseCase
import com.app.quickfun.domain.usecase.LoadVenueBookingsUseCase
import com.app.quickfun.domain.repository.PlaceRepository
import com.app.quickfun.domain.usecase.UpdatePlaceUseCase
import com.app.quickfun.ui.place.model.PlaceEffect
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.place.model.PlaceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class PlaceViewModel(
    application: Application,
    private val placeRepository: PlaceRepository,
    private val getPlacesUseCase: GetPlacesUseCase,
    private val getMyPlacesUseCase: GetMyPlacesUseCase,
    private val getPendingPlacesUseCase: GetPendingPlacesUseCase,
    private val approvePlaceUseCase: ApprovePlaceUseCase,
    private val createPlaceUseCase: CreatePlaceUseCase,
    private val updatePlaceUseCase: UpdatePlaceUseCase,
    private val getAvailableBookableSlotsUseCase: GetAvailableBookableSlotsUseCase,
    private val createBookingUseCase: CreateBookingUseCase,
    private val createWeekTimeSlotsUseCase: CreateWeekTimeSlotsUseCase,
    private val loadOwnerBookingScheduleUseCase: LoadOwnerBookingScheduleUseCase,
    private val loadVenueBookingsUseCase: LoadVenueBookingsUseCase,
    private val addSeatUseCase: AddSeatUseCase,
    private val generateVenueSeatLayoutUseCase: GenerateVenueSeatLayoutUseCase,
    private val listSeatsUseCase: ListSeatsUseCase,
    private val addCinemaSessionUseCase: AddCinemaSessionUseCase
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlaceViewModel"
    }

    private val _state = MutableStateFlow(PlaceState())
    val state: StateFlow<PlaceState> = _state

    private val _effects = MutableSharedFlow<PlaceEffect>()
    val effects: SharedFlow<PlaceEffect> = _effects.asSharedFlow()

    init {
        obtainEvent(PlaceIntent.Load)
    }

    fun obtainEvent(event: PlaceIntent) {
        when (event) {
            PlaceIntent.Load -> viewModelScope.launch { reloadPlacesList() }
            PlaceIntent.LoadMyPlaces -> viewModelScope.launch { reloadMyPlaces() }
            PlaceIntent.LoadModeration -> viewModelScope.launch { reloadModeration() }
            is PlaceIntent.ApprovePlace -> moderate(event)
            is PlaceIntent.AddPlace -> addPlace(event)
            is PlaceIntent.EditPlace -> editPlace(event)
            is PlaceIntent.UploadVenueCover -> uploadVenueCover(event)
            is PlaceIntent.AddVenueGalleryPhoto -> addVenueGalleryPhoto(event)
            is PlaceIntent.DeleteVenueGalleryPhoto -> deleteVenueGalleryPhoto(event)
            is PlaceIntent.LoadBookableSlots -> loadBookableSlots(event.placeId)
            PlaceIntent.CloseBookingDialog -> {
                _state.value = _state.value.copy(
                    bookingPlace = null,
                    bookableSlots = emptyList(),
                    bookingSeats = emptyList(),
                    isLoadingBookable = false
                )
            }
            is PlaceIntent.SubmitBooking -> submitBooking(event)
            is PlaceIntent.OpenOwnerSchedule -> openOwnerSchedule(event.placeId)
            PlaceIntent.CloseOwnerSchedule -> {
                _state.value = _state.value.copy(
                    ownerSchedulePlace = null,
                    ownerSeats = emptyList(),
                    ownerTimeSlots = emptyList(),
                    isLoadingOwnerSchedule = false,
                    isGeneratingVenueLayout = false,
                    isAddingCinemaSession = false
                )
            }
            is PlaceIntent.GenerateWeekSlots -> generateWeekSlots(event)
            is PlaceIntent.AddVenueSeat -> addVenueSeat(event)
            is PlaceIntent.GenerateVenueSeatLayout -> generateVenueSeatLayout(event)
            is PlaceIntent.AddCinemaSession -> addCinemaSession(event)
            is PlaceIntent.OpenVenueBookings -> openVenueBookings(event.placeId)
            PlaceIntent.CloseVenueBookings -> {
                _state.value = _state.value.copy(
                    venueBookingsPlace = null,
                    venueBookings = emptyList(),
                    isLoadingVenueBookings = false,
                    venueBookingsError = null
                )
            }
        }
    }

    private fun loadBookableSlots(placeId: String) {
        viewModelScope.launch {
            val place = _state.value.places.find { it.id == placeId }
                ?: _state.value.myPlaces.find { it.id == placeId }
            if (place == null) {
                _effects.emit(PlaceEffect.ShowMessage("Место не найдено в списке. Обновите каталог."))
                return@launch
            }
            _state.value = _state.value.copy(
                bookingPlace = place,
                isLoadingBookable = true,
                bookableSlots = emptyList(),
                bookingSeats = emptyList()
            )
            try {
                val list = withContext(Dispatchers.IO) {
                    getAvailableBookableSlotsUseCase(placeId)
                }
                val seats = withContext(Dispatchers.IO) {
                    when (place.resolveVenueKind()) {
                        VenueKind.CINEMA,
                        VenueKind.BOWLING,
                        VenueKind.BILLIARDS,
                        VenueKind.KARAOKE -> listSeatsUseCase(placeId)
                        else -> emptyList()
                    }
                }
                _state.value = _state.value.copy(
                    bookableSlots = list,
                    bookingSeats = seats,
                    isLoadingBookable = false
                )
                if (list.isEmpty()) {
                    _effects.emit(
                        PlaceEffect.ShowMessage(
                            "Нет свободных слотов. Выберите другое время или заведение."
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadBookableSlots failed", e)
                _state.value = _state.value.copy(
                    bookingPlace = null,
                    bookingSeats = emptyList(),
                    isLoadingBookable = false
                )
                _effects.emit(PlaceEffect.ShowMessage("Не удалось загрузить доступные слоты."))
            }
        }
    }

    private fun submitBooking(intent: PlaceIntent.SubmitBooking) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmittingBooking = true)
            try {
                withContext(Dispatchers.IO) {
                    createBookingUseCase(intent.timeSlotId, intent.seatId)
                }
                _effects.emit(PlaceEffect.ShowMessage("Бронь создана."))
                _state.value = _state.value.copy(
                    bookingPlace = null,
                    bookableSlots = emptyList(),
                    bookingSeats = emptyList(),
                    isSubmittingBooking = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "submitBooking failed", e)
                _state.value = _state.value.copy(isSubmittingBooking = false)
                val msg = e.message?.lowercase().orEmpty()
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        if ("duplicate" in msg || "unique" in msg || "already" in msg) {
                            "Этот слот уже занят. Обновите список."
                        } else {
                            "Не удалось создать бронь. Попробуйте снова."
                        }
                    )
                )
            }
        }
    }

    private fun openOwnerSchedule(placeId: String) {
        viewModelScope.launch {
            val place = _state.value.myPlaces.find { it.id == placeId }
            if (place == null) {
                _effects.emit(PlaceEffect.ShowMessage("Заведение не найдено среди ваших."))
                return@launch
            }
            _state.value = _state.value.copy(
                ownerSchedulePlace = place,
                isLoadingOwnerSchedule = true,
                venueBookingsPlace = null,
                venueBookings = emptyList(),
                venueBookingsError = null,
                isLoadingVenueBookings = false
            )
            try {
                val data = withContext(Dispatchers.IO) {
                    loadOwnerBookingScheduleUseCase(placeId)
                }
                _state.value = _state.value.copy(
                    ownerSeats = data.seats,
                    ownerTimeSlots = data.timeSlots,
                    isLoadingOwnerSchedule = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "openOwnerSchedule failed", e)
                _state.value = _state.value.copy(
                    ownerSchedulePlace = null,
                    isLoadingOwnerSchedule = false
                )
                _effects.emit(PlaceEffect.ShowMessage("Не удалось загрузить расписание."))
            }
        }
    }

    private fun openVenueBookings(placeId: String) {
        viewModelScope.launch {
            val place = _state.value.myPlaces.find { it.id == placeId }
            if (place == null) {
                _effects.emit(PlaceEffect.ShowMessage("Заведение не найдено среди ваших."))
                return@launch
            }
            _state.value = _state.value.copy(
                venueBookingsPlace = place,
                isLoadingVenueBookings = true,
                venueBookings = emptyList(),
                venueBookingsError = null,
                ownerSchedulePlace = null,
                ownerSeats = emptyList(),
                ownerTimeSlots = emptyList(),
                isLoadingOwnerSchedule = false
            )
            try {
                val list = withContext(Dispatchers.IO) {
                    loadVenueBookingsUseCase(placeId)
                }
                _state.value = _state.value.copy(
                    venueBookings = list,
                    isLoadingVenueBookings = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "openVenueBookings failed", e)
                _state.value = _state.value.copy(
                    isLoadingVenueBookings = false,
                    venueBookingsError = "Не удалось загрузить брони."
                )
            }
        }
    }

    private fun generateWeekSlots(intent: PlaceIntent.GenerateWeekSlots) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGeneratingSlots = true)
            try {
                withContext(Dispatchers.IO) {
                    createWeekTimeSlotsUseCase(
                        placeId = intent.placeId,
                        weekdays = intent.weekdays,
                        startHour = intent.startHour,
                        endHour = intent.endHour,
                        slotMinutes = intent.slotMinutes
                    )
                }
                _effects.emit(PlaceEffect.ShowMessage("Слоты на неделю добавлены."))
                val data = withContext(Dispatchers.IO) {
                    loadOwnerBookingScheduleUseCase(intent.placeId)
                }
                _state.value = _state.value.copy(
                    ownerSeats = data.seats,
                    ownerTimeSlots = data.timeSlots,
                    isGeneratingSlots = false
                )
                reloadPlacesList()
                reloadMyPlaces()
            } catch (e: Exception) {
                Log.e(TAG, "generateWeekSlots failed", e)
                _state.value = _state.value.copy(isGeneratingSlots = false)
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        e.message ?: "Не удалось создать слоты."
                    )
                )
            }
        }
    }

    private fun addVenueSeat(intent: PlaceIntent.AddVenueSeat) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAddingSeat = true)
            try {
                withContext(Dispatchers.IO) {
                    addSeatUseCase(intent.placeId, intent.row, intent.seatNum)
                }
                _effects.emit(PlaceEffect.ShowMessage("Место добавлено."))
                val data = withContext(Dispatchers.IO) {
                    loadOwnerBookingScheduleUseCase(intent.placeId)
                }
                _state.value = _state.value.copy(
                    ownerSeats = data.seats,
                    ownerTimeSlots = data.timeSlots,
                    isAddingSeat = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "addVenueSeat failed", e)
                _state.value = _state.value.copy(isAddingSeat = false)
                _effects.emit(PlaceEffect.ShowMessage("Не удалось добавить место."))
            }
        }
    }

    private fun addCinemaSession(intent: PlaceIntent.AddCinemaSession) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAddingCinemaSession = true)
            try {
                val zone = ZoneId.systemDefault()
                val date = LocalDate.parse(intent.dateYmd.trim())
                val start = LocalDateTime.of(date, LocalTime.of(intent.startHour, intent.startMinute))
                val end = LocalDateTime.of(date, LocalTime.of(intent.endHour, intent.endMinute))
                if (!end.isAfter(start)) {
                    throw IllegalStateException("Конец сеанса должен быть позже начала (тот же день).")
                }
                val utcStart = start.atZone(zone).withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val utcEnd = end.atZone(zone).withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                withContext(Dispatchers.IO) {
                    addCinemaSessionUseCase(intent.placeId, intent.filmTitle, utcStart, utcEnd)
                }
                _effects.emit(PlaceEffect.ShowMessage("Сеанс добавлен."))
                if (_state.value.ownerSchedulePlace?.id == intent.placeId) {
                    val data = withContext(Dispatchers.IO) {
                        loadOwnerBookingScheduleUseCase(intent.placeId)
                    }
                    _state.value = _state.value.copy(
                        ownerSeats = data.seats,
                        ownerTimeSlots = data.timeSlots,
                        isAddingCinemaSession = false
                    )
                } else {
                    _state.value = _state.value.copy(isAddingCinemaSession = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "addCinemaSession failed", e)
                _state.value = _state.value.copy(isAddingCinemaSession = false)
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Не удалось добавить сеанс."))
            }
        }
    }

    private fun generateVenueSeatLayout(intent: PlaceIntent.GenerateVenueSeatLayout) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGeneratingVenueLayout = true)
            try {
                withContext(Dispatchers.IO) {
                    generateVenueSeatLayoutUseCase(intent.placeId, intent.config)
                }
                _effects.emit(PlaceEffect.ShowMessage("Схема мест создана."))
                val data = withContext(Dispatchers.IO) {
                    loadOwnerBookingScheduleUseCase(intent.placeId)
                }
                _state.value = _state.value.copy(
                    ownerSeats = data.seats,
                    ownerTimeSlots = data.timeSlots,
                    isGeneratingVenueLayout = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "generateVenueSeatLayout failed", e)
                _state.value = _state.value.copy(isGeneratingVenueLayout = false)
                _effects.emit(
                    PlaceEffect.ShowMessage(e.message ?: "Не удалось создать схему мест.")
                )
            }
        }
    }

    private fun moderate(intent: PlaceIntent.ApprovePlace) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isModerating = true)
            try {
                withContext(Dispatchers.IO) {
                    approvePlaceUseCase(intent.placeId, intent.approved)
                }
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        if (intent.approved) "Заведение одобрено." else "Заявка отклонена."
                    )
                )
                reloadModeration()
                reloadPlacesList()
                reloadMyPlaces()
            } catch (e: Exception) {
                Log.e(TAG, "moderate failed", e)
                _effects.emit(PlaceEffect.ShowMessage("Не удалось обновить статус заявки."))
            } finally {
                _state.value = _state.value.copy(isModerating = false)
            }
        }
    }

    private suspend fun reloadModeration() {
        _state.value = _state.value.copy(isLoadingModeration = true)
        try {
            val list = withContext(Dispatchers.IO) { getPendingPlacesUseCase() }
            _state.value = _state.value.copy(pendingPlaces = list, isLoadingModeration = false)
        } catch (e: Exception) {
            Log.e(TAG, "reloadModeration failed", e)
            _state.value = _state.value.copy(isLoadingModeration = false)
            _effects.emit(PlaceEffect.ShowMessage("Не удалось загрузить заявки."))
        }
    }

    private fun readAllBytes(uri: Uri): ByteArray? =
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }

    private fun extensionFromMime(mime: String?): String {
        val m = mime?.lowercase().orEmpty()
        return when {
            "png" in m -> "png"
            "webp" in m -> "webp"
            else -> "jpg"
        }
    }

    private fun uploadVenueCover(intent: PlaceIntent.UploadVenueCover) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingPlaceMedia = true)
            try {
                val bytes = readAllBytes(intent.uri)
                    ?: throw IllegalStateException("Не удалось прочитать файл.")
                val mime = getApplication<Application>().contentResolver.getType(intent.uri)
                val ext = extensionFromMime(mime)
                val url = withContext(Dispatchers.IO) {
                    placeRepository.uploadVenueImageToStorage(intent.placeId, bytes, ext)
                }
                withContext(Dispatchers.IO) {
                    placeRepository.updatePlaceCoverUrl(intent.placeId, url)
                }
                _effects.emit(PlaceEffect.ShowMessage("Обложка обновлена."))
                reloadPlacesList()
                reloadMyPlaces()
                reloadModeration()
            } catch (e: Exception) {
                Log.e(TAG, "uploadVenueCover failed", e)
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: "Не удалось загрузить обложку."
                    )
                )
            } finally {
                _state.value = _state.value.copy(isUploadingPlaceMedia = false)
            }
        }
    }

    private fun addVenueGalleryPhoto(intent: PlaceIntent.AddVenueGalleryPhoto) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingPlaceMedia = true)
            try {
                val bytes = readAllBytes(intent.uri)
                    ?: throw IllegalStateException("Не удалось прочитать файл.")
                val mime = getApplication<Application>().contentResolver.getType(intent.uri)
                val ext = extensionFromMime(mime)
                val url = withContext(Dispatchers.IO) {
                    placeRepository.uploadVenueImageToStorage(intent.placeId, bytes, ext)
                }
                withContext(Dispatchers.IO) {
                    placeRepository.insertPlaceGalleryPhoto(intent.placeId, url)
                }
                _effects.emit(PlaceEffect.ShowMessage("Фото добавлено в галерею."))
                reloadPlacesList()
                reloadMyPlaces()
                reloadModeration()
            } catch (e: Exception) {
                Log.e(TAG, "addVenueGalleryPhoto failed", e)
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: "Не удалось добавить фото (максимум 3 в галерее)."
                    )
                )
            } finally {
                _state.value = _state.value.copy(isUploadingPlaceMedia = false)
            }
        }
    }

    private fun deleteVenueGalleryPhoto(intent: PlaceIntent.DeleteVenueGalleryPhoto) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingPlaceMedia = true)
            try {
                withContext(Dispatchers.IO) {
                    placeRepository.deletePlaceGalleryPhoto(intent.photoId)
                }
                _effects.emit(PlaceEffect.ShowMessage("Фото удалено."))
                reloadPlacesList()
                reloadMyPlaces()
                reloadModeration()
            } catch (e: Exception) {
                Log.e(TAG, "deleteVenueGalleryPhoto failed", e)
                _effects.emit(PlaceEffect.ShowMessage("Не удалось удалить фото."))
            } finally {
                _state.value = _state.value.copy(isUploadingPlaceMedia = false)
            }
        }
    }

    private fun editPlace(intent: PlaceIntent.EditPlace) {
        viewModelScope.launch {
            if (intent.name.isBlank()) {
                _effects.emit(PlaceEffect.ShowMessage("Укажите название места."))
                return@launch
            }
            _state.value = _state.value.copy(isEditingPlace = true)
            try {
                withContext(Dispatchers.IO) {
                    updatePlaceUseCase(
                        placeId = intent.placeId,
                        locationId = intent.locationId,
                        name = intent.name,
                        description = intent.description,
                        city = intent.city,
                        address = intent.address,
                        categoryId = intent.categoryId,
                        latitude = intent.latitude,
                        longitude = intent.longitude,
                        status = intent.status,
                        coverImageUrl = intent.coverImageUrl
                    )
                }
                _effects.emit(PlaceEffect.ShowMessage("Изменения сохранены."))
                _effects.emit(PlaceEffect.EditPlaceSucceeded)
                reloadPlacesList()
                reloadModeration()
                reloadMyPlaces()
            } catch (e: IllegalArgumentException) {
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Проверьте координаты и адрес."))
            } catch (e: Exception) {
                Log.e(TAG, "editPlace failed", e)
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        "Не удалось сохранить. Проверьте права и данные."
                    )
                )
            } finally {
                _state.value = _state.value.copy(isEditingPlace = false)
            }
        }
    }

    private fun addPlace(intent: PlaceIntent.AddPlace) {
        viewModelScope.launch {
            if (intent.name.isBlank()) {
                _effects.emit(PlaceEffect.ShowMessage("Укажите название места."))
                return@launch
            }
            _state.value = _state.value.copy(isAddingPlace = true)
            try {
                withContext(Dispatchers.IO) {
                    createPlaceUseCase(
                        name = intent.name,
                        description = intent.description,
                        city = intent.city,
                        address = intent.address,
                        categoryId = intent.categoryId,
                        latitude = intent.latitude,
                        longitude = intent.longitude
                    )
                }
                _effects.emit(PlaceEffect.ShowMessage("Место добавлено."))
                _effects.emit(PlaceEffect.AddPlaceSucceeded)
                reloadPlacesList()
                reloadMyPlaces()
            } catch (e: IllegalArgumentException) {
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Проверьте координаты и адрес."))
            } catch (e: Exception) {
                Log.e(TAG, "addPlace failed", e)
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        "Не удалось добавить место. Нужны права администратора или проверьте данные."
                    )
                )
            } finally {
                _state.value = _state.value.copy(isAddingPlace = false)
            }
        }
    }

    private suspend fun reloadPlacesList() {
        _state.value = _state.value.copy(isLoading = true)
        try {
            val list = withContext(Dispatchers.IO) { getPlacesUseCase() }
            _state.value = _state.value.copy(places = list, isLoading = false)
        } catch (e: Exception) {
            Log.e(TAG, "reloadPlacesList failed", e)
            _state.value = _state.value.copy(isLoading = false)
            _effects.emit(PlaceEffect.ShowMessage("Не удалось загрузить список мест"))
        }
    }

    private suspend fun reloadMyPlaces() {
        _state.value = _state.value.copy(isLoadingMyPlaces = true)
        try {
            val list = withContext(Dispatchers.IO) { getMyPlacesUseCase() }
            _state.value = _state.value.copy(myPlaces = list, isLoadingMyPlaces = false)
        } catch (e: Exception) {
            Log.e(TAG, "reloadMyPlaces failed", e)
            _state.value = _state.value.copy(isLoadingMyPlaces = false)
            _effects.emit(PlaceEffect.ShowMessage("Не удалось загрузить ваши заведения"))
        }
    }
}
