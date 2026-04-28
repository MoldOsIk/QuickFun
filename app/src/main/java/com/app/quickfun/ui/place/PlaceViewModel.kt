package com.app.quickfun.ui.place

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.quickfun.data.local.MyPlacesModerationStatusStore
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.VenueKind
import com.app.quickfun.domain.model.resolveVenueKind
import com.app.quickfun.domain.usecase.AddCinemaSeatAtCellUseCase
import com.app.quickfun.domain.usecase.AddCinemaSessionUseCase
import com.app.quickfun.domain.usecase.AddSeatUseCase
import com.app.quickfun.domain.usecase.ClearCinemaHallUseCase
import com.app.quickfun.domain.usecase.GenerateVenueSeatLayoutUseCase
import com.app.quickfun.domain.usecase.ListSeatsUseCase
import com.app.quickfun.domain.usecase.SaveCinemaSeatLayoutUseCase
import com.app.quickfun.domain.usecase.ApprovePlaceUseCase
import com.app.quickfun.domain.usecase.CancelBookingUseCase
import com.app.quickfun.domain.usecase.CreateBookingUseCase
import com.app.quickfun.domain.usecase.CreatePlaceUseCase
import com.app.quickfun.domain.usecase.CreateWeekTimeSlotsUseCase
import com.app.quickfun.domain.usecase.DeleteCinemaSessionUseCase
import com.app.quickfun.domain.usecase.DeleteVenueSeatUseCase
import com.app.quickfun.domain.usecase.GetAvailableBookableSlotsUseCase
import com.app.quickfun.domain.usecase.GetMyPlacesUseCase
import com.app.quickfun.domain.usecase.GetPendingPlacesUseCase
import com.app.quickfun.domain.usecase.GetPlaceReviewsUseCase
import com.app.quickfun.domain.usecase.GetPlacesUseCase
import com.app.quickfun.domain.usecase.GetVenueCategoriesUseCase
import com.app.quickfun.domain.usecase.ResubmitRejectedPlaceUseCase
import com.app.quickfun.domain.usecase.LoadOwnerBookingScheduleUseCase
import com.app.quickfun.domain.usecase.LoadVenueBookingsUseCase
import com.app.quickfun.domain.repository.PlaceRepository
import com.app.quickfun.domain.usecase.UpdateCinemaSessionUseCase
import com.app.quickfun.domain.usecase.UpdatePlaceUseCase
import com.app.quickfun.domain.usecase.UpsertPlaceReviewUseCase
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
    private val myPlacesModerationStatusStore: MyPlacesModerationStatusStore,
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
    private val cancelBookingUseCase: CancelBookingUseCase,
    private val addSeatUseCase: AddSeatUseCase,
    private val generateVenueSeatLayoutUseCase: GenerateVenueSeatLayoutUseCase,
    private val listSeatsUseCase: ListSeatsUseCase,
    private val saveCinemaSeatLayoutUseCase: SaveCinemaSeatLayoutUseCase,
    private val addCinemaSessionUseCase: AddCinemaSessionUseCase,
    private val updateCinemaSessionUseCase: UpdateCinemaSessionUseCase,
    private val deleteCinemaSessionUseCase: DeleteCinemaSessionUseCase,
    private val clearCinemaHallUseCase: ClearCinemaHallUseCase,
    private val deleteVenueSeatUseCase: DeleteVenueSeatUseCase,
    private val addCinemaSeatAtCellUseCase: AddCinemaSeatAtCellUseCase,
    private val getVenueCategoriesUseCase: GetVenueCategoriesUseCase,
    private val resubmitRejectedPlaceUseCase: ResubmitRejectedPlaceUseCase,
    private val getPlaceReviewsUseCase: GetPlaceReviewsUseCase,
    private val upsertPlaceReviewUseCase: UpsertPlaceReviewUseCase
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlaceViewModel"
    }

    private val _state = MutableStateFlow(PlaceState())
    val state: StateFlow<PlaceState> = _state

    private val _effects = MutableSharedFlow<PlaceEffect>(extraBufferCapacity = 32)
    val effects: SharedFlow<PlaceEffect> = _effects.asSharedFlow()

    init {
        obtainEvent(PlaceIntent.Load)
    }

    fun obtainEvent(event: PlaceIntent) {
        when (event) {
            PlaceIntent.Load -> viewModelScope.launch { reloadPlacesList() }
            PlaceIntent.LoadMyPlaces -> viewModelScope.launch { reloadMyPlaces() }
            PlaceIntent.LoadModeration -> viewModelScope.launch { reloadModeration() }
            PlaceIntent.LoadVenueCategories -> loadVenueCategories()
            is PlaceIntent.ApprovePlace -> moderate(event)
            is PlaceIntent.ResubmitRejectedPlace -> resubmitRejectedPlace(event.placeId)
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
                    isSavingSeatLayout = false,
                    isAddingCinemaSession = false,
                    isUpdatingCinemaSession = false,
                    isDeletingCinemaSession = false,
                    isClearingCinemaHall = false,
                    isDeletingVenueSeat = false
                )
            }
            is PlaceIntent.GenerateWeekSlots -> generateWeekSlots(event)
            is PlaceIntent.AddVenueSeat -> addVenueSeat(event)
            is PlaceIntent.GenerateVenueSeatLayout -> generateVenueSeatLayout(event)
            is PlaceIntent.SaveCinemaSeatLayout -> saveCinemaSeatLayout(event)
            is PlaceIntent.AddCinemaSession -> addCinemaSession(event)
            is PlaceIntent.UpdateCinemaSession -> updateCinemaSession(event)
            is PlaceIntent.DeleteCinemaSession -> deleteCinemaSession(event)
            is PlaceIntent.ClearCinemaHall -> clearCinemaHall(event)
            is PlaceIntent.DeleteVenueSeat -> deleteVenueSeat(event)
            is PlaceIntent.AddCinemaSeatAtCell -> addCinemaSeatAtCell(event)
            is PlaceIntent.OpenVenueBookings -> openVenueBookings(event.placeId)
            PlaceIntent.CloseVenueBookings -> {
                _state.value = _state.value.copy(
                    venueBookingsPlace = null,
                    venueBookings = emptyList(),
                    isLoadingVenueBookings = false,
                    venueBookingsError = null,
                    cancellingVenueBookingId = null
                )
            }
            is PlaceIntent.CancelVenueBooking -> cancelVenueBooking(event.bookingId)
            is PlaceIntent.FocusPlaceOnInAppMap -> {
                _state.value = _state.value.copy(pendingMapFocusPlaceId = event.placeId)
            }
            PlaceIntent.ConsumeMapFocusRequest -> {
                _state.value = _state.value.copy(pendingMapFocusPlaceId = null)
            }
            is PlaceIntent.OpenPlaceReviews -> openPlaceReviews(event.placeId)
            PlaceIntent.ClosePlaceReviews -> closePlaceReviews()
            is PlaceIntent.SubmitPlaceReview -> submitPlaceReview(event)
        }
    }

    private fun loadVenueCategories() {
        viewModelScope.launch {
            if (_state.value.isLoadingVenueCategories) return@launch
            _state.value = _state.value.copy(isLoadingVenueCategories = true)
            try {
                val list = withContext(Dispatchers.IO) { getVenueCategoriesUseCase() }
                _state.value = _state.value.copy(venueCategories = list, isLoadingVenueCategories = false)
            } catch (e: Exception) {
                Log.e(TAG, "loadVenueCategories failed", e)
                _state.value = _state.value.copy(isLoadingVenueCategories = false)
                _effects.emit(PlaceEffect.ShowMessage("Не удалось загрузить категории."))
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
                _effects.emit(PlaceEffect.ShowMessage("Бронь создана. Список активных броней — в профиле."))
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

    private fun cancelVenueBooking(bookingId: Int) {
        viewModelScope.launch {
            val place = _state.value.venueBookingsPlace ?: return@launch
            _state.value = _state.value.copy(cancellingVenueBookingId = bookingId)
            try {
                withContext(Dispatchers.IO) { cancelBookingUseCase(bookingId) }
                val list = withContext(Dispatchers.IO) { loadVenueBookingsUseCase(place.id) }
                _state.value = _state.value.copy(
                    venueBookings = list,
                    cancellingVenueBookingId = null
                )
                _effects.emit(PlaceEffect.ShowMessage("Бронь отменена."))
            } catch (e: Exception) {
                Log.e(TAG, "cancelVenueBooking failed", e)
                _state.value = _state.value.copy(cancellingVenueBookingId = null)
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        "Не удалось отменить бронь. Попробуйте снова или обновите список."
                    )
                )
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
                cancellingVenueBookingId = null,
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
            val place = _state.value.myPlaces.find { it.id == intent.placeId }
            if (place?.resolveVenueKind() == VenueKind.CINEMA) {
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        "Для кино недельная сетка не используется — добавляйте сеансы фильмов в блоке ниже."
                    )
                )
                return@launch
            }
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

    private fun updateCinemaSession(intent: PlaceIntent.UpdateCinemaSession) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdatingCinemaSession = true)
            try {
                withContext(Dispatchers.IO) {
                    updateCinemaSessionUseCase(
                        placeId = intent.placeId,
                        timeSlotId = intent.timeSlotId,
                        filmTitle = intent.filmTitle,
                        dateYmd = intent.dateYmd,
                        startHour = intent.startHour,
                        startMinute = intent.startMinute,
                        endHour = intent.endHour,
                        endMinute = intent.endMinute
                    )
                }
                _effects.emit(PlaceEffect.ShowMessage("Сеанс обновлён."))
                reloadOwnerIfOpen(intent.placeId)
            } catch (e: Exception) {
                Log.e(TAG, "updateCinemaSession failed", e)
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Не удалось обновить сеанс."))
            } finally {
                _state.value = _state.value.copy(isUpdatingCinemaSession = false)
            }
        }
    }

    private fun deleteCinemaSession(intent: PlaceIntent.DeleteCinemaSession) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeletingCinemaSession = true)
            try {
                withContext(Dispatchers.IO) {
                    deleteCinemaSessionUseCase(intent.timeSlotId)
                }
                _effects.emit(PlaceEffect.ShowMessage("Сеанс удалён."))
                reloadOwnerIfOpen(intent.placeId)
            } catch (e: Exception) {
                Log.e(TAG, "deleteCinemaSession failed", e)
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Не удалось удалить сеанс."))
            } finally {
                _state.value = _state.value.copy(isDeletingCinemaSession = false)
            }
        }
    }

    private fun clearCinemaHall(intent: PlaceIntent.ClearCinemaHall) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClearingCinemaHall = true)
            try {
                withContext(Dispatchers.IO) {
                    clearCinemaHallUseCase(intent.placeId)
                }
                _effects.emit(PlaceEffect.ShowMessage("Зал очищен: все места удалены из базы."))
                reloadOwnerIfOpen(intent.placeId)
            } catch (e: Exception) {
                Log.e(TAG, "clearCinemaHall failed", e)
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Не удалось очистить зал."))
            } finally {
                _state.value = _state.value.copy(isClearingCinemaHall = false)
            }
        }
    }

    private fun deleteVenueSeat(intent: PlaceIntent.DeleteVenueSeat) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeletingVenueSeat = true)
            try {
                withContext(Dispatchers.IO) {
                    deleteVenueSeatUseCase(intent.seatId)
                }
                _effects.emit(PlaceEffect.ShowMessage("Место удалено."))
                reloadOwnerIfOpen(intent.placeId)
            } catch (e: Exception) {
                Log.e(TAG, "deleteVenueSeat failed", e)
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Не удалось удалить место."))
            } finally {
                _state.value = _state.value.copy(isDeletingVenueSeat = false)
            }
        }
    }

    private fun addCinemaSeatAtCell(intent: PlaceIntent.AddCinemaSeatAtCell) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAddingSeat = true)
            try {
                withContext(Dispatchers.IO) {
                    addCinemaSeatAtCellUseCase(
                        intent.placeId,
                        intent.rowNumber,
                        intent.seatNumber,
                        intent.layoutX,
                        intent.layoutY
                    )
                }
                _effects.emit(PlaceEffect.ShowMessage("Место добавлено."))
                reloadOwnerIfOpen(intent.placeId)
            } catch (e: IllegalArgumentException) {
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Проверьте ряд и номер."))
            } catch (e: Exception) {
                Log.e(TAG, "addCinemaSeatAtCell failed", e)
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Не удалось добавить место."))
            } finally {
                _state.value = _state.value.copy(isAddingSeat = false)
            }
        }
    }

    private suspend fun reloadOwnerIfOpen(placeId: String) {
        if (_state.value.ownerSchedulePlace?.id != placeId) return
        val data = withContext(Dispatchers.IO) {
            loadOwnerBookingScheduleUseCase(placeId)
        }
        _state.value = _state.value.copy(
            ownerSeats = data.seats,
            ownerTimeSlots = data.timeSlots
        )
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

    private fun saveCinemaSeatLayout(intent: PlaceIntent.SaveCinemaSeatLayout) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingSeatLayout = true)
            try {
                withContext(Dispatchers.IO) {
                    saveCinemaSeatLayoutUseCase(intent.placeId, intent.positions)
                }
                _effects.emit(PlaceEffect.ShowMessage("Схема зала сохранена."))
                _effects.emit(PlaceEffect.CinemaSeatLayoutSaved)
                val data = withContext(Dispatchers.IO) {
                    loadOwnerBookingScheduleUseCase(intent.placeId)
                }
                _state.value = _state.value.copy(
                    ownerSeats = data.seats,
                    ownerTimeSlots = data.timeSlots,
                    isSavingSeatLayout = false
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "saveCinemaSeatLayout validation", e)
                _state.value = _state.value.copy(isSavingSeatLayout = false)
                _effects.emit(PlaceEffect.ShowMessage(e.message ?: "Проверьте схему мест."))
            } catch (e: Exception) {
                Log.e(TAG, "saveCinemaSeatLayout failed", e)
                _state.value = _state.value.copy(isSavingSeatLayout = false)
                _effects.emit(
                    PlaceEffect.ShowMessage(e.message ?: "Не удалось сохранить схему зала.")
                )
            }
        }
    }

    private fun moderate(intent: PlaceIntent.ApprovePlace) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isModerating = true)
            try {
                withContext(Dispatchers.IO) {
                    approvePlaceUseCase(
                        intent.placeId,
                        intent.approved,
                        intent.rejectionReason?.trim()?.takeIf { it.isNotEmpty() }
                    )
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
            val (persisted, list) = withContext(Dispatchers.IO) {
                val p = myPlacesModerationStatusStore.loadStatusByPlaceId()
                val l = getMyPlacesUseCase()
                p to l
            }
            val newlyApproved = mutableListOf<Place>()
            val newlyRejected = mutableListOf<Place>()
            for (p in list) {
                val oldStatus = persisted[p.id] ?: continue
                val ns = p.status?.lowercase()?.trim().orEmpty()
                if (oldStatus == ns) continue
                when (ns) {
                    "approved" -> newlyApproved.add(p)
                    "rejected" -> newlyRejected.add(p)
                    else -> { /* pending и др. — без snackbar */ }
                }
            }
            when {
                newlyApproved.size == 1 -> {
                    val p = newlyApproved[0]
                    _effects.emit(
                        PlaceEffect.ShowMessage(
                            "Заведение «${p.name}» одобрено и доступно в каталоге."
                        )
                    )
                }
                newlyApproved.size > 1 -> {
                    val names = newlyApproved.joinToString(", ") { "«${it.name}»" }
                    _effects.emit(
                        PlaceEffect.ShowMessage(
                            "Одобрены заявки (${newlyApproved.size}): $names. Они доступны в каталоге."
                        )
                    )
                }
            }
            when {
                newlyRejected.size == 1 -> {
                    val p = newlyRejected[0]
                    val r = p.rejectionReason?.trim()?.takeIf { it.isNotEmpty() }
                    val msg = if (r != null) {
                        "Заявка «${p.name}» отклонена.\nПричина: $r"
                    } else {
                        "Заявка «${p.name}» отклонена."
                    }
                    _effects.emit(PlaceEffect.ShowMessage(msg))
                }
                newlyRejected.size > 1 -> {
                    val body = newlyRejected.joinToString("\n") { p ->
                        val r = p.rejectionReason?.trim()?.takeIf { it.isNotEmpty() }
                        if (r != null) "«${p.name}»: $r" else "«${p.name}»"
                    }
                    _effects.emit(
                        PlaceEffect.ShowMessage(
                            "Отклонены заявки (${newlyRejected.size}):\n$body"
                        )
                    )
                }
            }
            withContext(Dispatchers.IO) {
                myPlacesModerationStatusStore.saveStatusByPlaceId(list)
            }
            _state.value = _state.value.copy(myPlaces = list, isLoadingMyPlaces = false)
        } catch (e: Exception) {
            Log.e(TAG, "reloadMyPlaces failed", e)
            _state.value = _state.value.copy(isLoadingMyPlaces = false)
            _effects.emit(PlaceEffect.ShowMessage("Не удалось загрузить ваши заведения"))
        }
    }

    private fun resubmitRejectedPlace(placeId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { resubmitRejectedPlaceUseCase(placeId) }
                _effects.emit(PlaceEffect.ShowMessage("Заявка снова на модерации."))
                reloadMyPlaces()
                reloadModeration()
                reloadPlacesList()
            } catch (e: Exception) {
                Log.e(TAG, "resubmitRejectedPlace failed", e)
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        "Не удалось отправить повторно. Проверьте, что заведение отклонено и вы владелец."
                    )
                )
            }
        }
    }

    private fun openPlaceReviews(placeId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                reviewsPlaceId = placeId,
                placeReviews = emptyList(),
                isLoadingPlaceReviews = true
            )
            try {
                val list = withContext(Dispatchers.IO) { getPlaceReviewsUseCase(placeId) }
                _state.value = _state.value.copy(
                    placeReviews = list,
                    isLoadingPlaceReviews = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "openPlaceReviews failed", e)
                _state.value = _state.value.copy(
                    reviewsPlaceId = null,
                    placeReviews = emptyList(),
                    isLoadingPlaceReviews = false
                )
                _effects.emit(PlaceEffect.ShowMessage("Не удалось загрузить отзывы."))
            }
        }
    }

    private fun closePlaceReviews() {
        _state.value = _state.value.copy(
            reviewsPlaceId = null,
            placeReviews = emptyList(),
            isLoadingPlaceReviews = false,
            isSubmittingPlaceReview = false
        )
    }

    private fun submitPlaceReview(event: PlaceIntent.SubmitPlaceReview) {
        viewModelScope.launch {
            val trimmed = event.body.trim()
            if (trimmed.length < 3) {
                _effects.emit(PlaceEffect.ShowMessage("Текст отзыва — не короче 3 символов."))
                return@launch
            }
            if (event.rating !in 1..10) {
                _effects.emit(PlaceEffect.ShowMessage("Оценка должна быть от 1 до 10."))
                return@launch
            }
            _state.value = _state.value.copy(isSubmittingPlaceReview = true)
            try {
                withContext(Dispatchers.IO) {
                    upsertPlaceReviewUseCase(event.placeId, event.rating, trimmed)
                }
                val list = withContext(Dispatchers.IO) { getPlaceReviewsUseCase(event.placeId) }
                _state.value = _state.value.copy(
                    placeReviews = list,
                    isSubmittingPlaceReview = false
                )
                _effects.emit(PlaceEffect.ShowMessage("Отзыв сохранён."))
            } catch (e: Exception) {
                Log.e(TAG, "submitPlaceReview failed", e)
                _state.value = _state.value.copy(isSubmittingPlaceReview = false)
                _effects.emit(
                    PlaceEffect.ShowMessage(
                        "Не удалось сохранить отзыв. Войдите в аккаунт, " +
                            "убедитесь, что заведение одобрено, и что вы не владелец этой площадки."
                    )
                )
            }
        }
    }
}
