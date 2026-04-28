package com.app.quickfun.ui.place.model

import com.app.quickfun.domain.model.BookableSlot
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.PlaceReview
import com.app.quickfun.domain.model.VenueCategory
import com.app.quickfun.domain.model.Seat
import com.app.quickfun.domain.model.TimeSlot
import com.app.quickfun.domain.model.VenueBooking

data class PlaceState(
    val places: List<Place> = emptyList(),
    val myPlaces: List<Place> = emptyList(),
    val pendingPlaces: List<Place> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMyPlaces: Boolean = false,
    val isLoadingModeration: Boolean = false,
    val isAddingPlace: Boolean = false,
    val isEditingPlace: Boolean = false,
    val isUploadingPlaceMedia: Boolean = false,
    val isModerating: Boolean = false,

    val bookingPlace: Place? = null,
    val bookableSlots: List<BookableSlot> = emptyList(),
    /** Для схемы зала (кино) и списка дорожек. */
    val bookingSeats: List<Seat> = emptyList(),
    val isLoadingBookable: Boolean = false,
    val isSubmittingBooking: Boolean = false,

    val ownerSchedulePlace: Place? = null,
    val ownerSeats: List<Seat> = emptyList(),
    val ownerTimeSlots: List<TimeSlot> = emptyList(),
    val isLoadingOwnerSchedule: Boolean = false,
    val isGeneratingSlots: Boolean = false,
    val isAddingSeat: Boolean = false,
    val isGeneratingVenueLayout: Boolean = false,
    val isSavingSeatLayout: Boolean = false,
    val isAddingCinemaSession: Boolean = false,
    val isUpdatingCinemaSession: Boolean = false,
    val isDeletingCinemaSession: Boolean = false,
    val isClearingCinemaHall: Boolean = false,
    val isDeletingVenueSeat: Boolean = false,

    val venueBookingsPlace: Place? = null,
    val venueBookings: List<VenueBooking> = emptyList(),
    val isLoadingVenueBookings: Boolean = false,
    val venueBookingsError: String? = null,
    /** Ид брони, для которой идёт отмена (кнопка загрузки). */
    val cancellingVenueBookingId: Int? = null,

    /** Переключение на вкладку «Карта» и приближение к этому заведению (обрабатывает [PlaceMapTab]). */
    val pendingMapFocusPlaceId: String? = null,

    val venueCategories: List<VenueCategory> = emptyList(),
    val isLoadingVenueCategories: Boolean = false,

    /** Экран отзывов: null — закрыт. */
    val reviewsPlaceId: String? = null,
    val placeReviews: List<PlaceReview> = emptyList(),
    val isLoadingPlaceReviews: Boolean = false,
    val isSubmittingPlaceReview: Boolean = false
)
