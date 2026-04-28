package com.app.quickfun.di

import com.app.quickfun.data.repository.AuthRepositoryImpl
import com.app.quickfun.data.repository.BookingRepositoryImpl
import com.app.quickfun.data.repository.PlaceRepositoryImpl
import com.app.quickfun.data.repository.ProfileRepositoryImpl
import com.app.quickfun.domain.usecase.AddSeatUseCase
import com.app.quickfun.domain.usecase.ApprovePlaceUseCase
import com.app.quickfun.domain.usecase.CreateBookingUseCase
import com.app.quickfun.domain.usecase.ClearCinemaHallUseCase
import com.app.quickfun.domain.usecase.CreateWeekTimeSlotsUseCase
import com.app.quickfun.domain.usecase.DeleteCinemaSessionUseCase
import com.app.quickfun.domain.usecase.DeleteVenueSeatUseCase
import com.app.quickfun.domain.usecase.AddCinemaSeatAtCellUseCase
import com.app.quickfun.domain.usecase.AddCinemaSessionUseCase
import com.app.quickfun.domain.usecase.GenerateVenueSeatLayoutUseCase
import com.app.quickfun.domain.usecase.ListSeatsUseCase
import com.app.quickfun.domain.usecase.SaveCinemaSeatLayoutUseCase
import com.app.quickfun.domain.usecase.GetAvailableBookableSlotsUseCase
import com.app.quickfun.domain.usecase.LoadOwnerBookingScheduleUseCase
import com.app.quickfun.domain.usecase.LoadVenueBookingsUseCase
import com.app.quickfun.domain.usecase.CancelBookingUseCase
import com.app.quickfun.domain.usecase.CheckAuthUseCase
import com.app.quickfun.domain.usecase.CreatePlaceUseCase
import com.app.quickfun.domain.usecase.GetMyPlacesUseCase
import com.app.quickfun.domain.usecase.GetMyActiveBookingsUseCase
import com.app.quickfun.domain.usecase.GetPendingPlacesUseCase
import com.app.quickfun.domain.usecase.GetPlacesUseCase
import com.app.quickfun.domain.usecase.GetVenueCategoriesUseCase
import com.app.quickfun.domain.usecase.RegisterPlaceAsOwnerUseCase
import com.app.quickfun.domain.usecase.ResubmitRejectedPlaceUseCase
import com.app.quickfun.domain.usecase.UpdateCinemaSessionUseCase
import com.app.quickfun.domain.usecase.UpdatePlaceUseCase
import com.app.quickfun.domain.usecase.UpsertPlaceReviewUseCase
import com.app.quickfun.domain.usecase.GetPlaceReviewsUseCase
import com.app.quickfun.domain.usecase.GetProfileUseCase
import com.app.quickfun.domain.usecase.SaveProfileNameUseCase
import com.app.quickfun.domain.usecase.SaveProfilePhoneUseCase
import com.app.quickfun.domain.usecase.SignInUseCase
import com.app.quickfun.domain.usecase.SignOutUseCase
import com.app.quickfun.domain.usecase.SignUpUseCase
import com.app.quickfun.domain.repository.PlaceRepository

object AppModule {

    private val placeRepositoryImpl = PlaceRepositoryImpl()

    /** Для загрузки фото заведений из ViewModel (остальное — через use case). */
    fun placeRepository(): PlaceRepository = placeRepositoryImpl
    private val bookingRepository = BookingRepositoryImpl()
    private val Authrepository = AuthRepositoryImpl()
    private val profileRepository = ProfileRepositoryImpl()

    val getPlacesUseCase = GetPlacesUseCase(placeRepositoryImpl)
    val getVenueCategoriesUseCase = GetVenueCategoriesUseCase(placeRepositoryImpl)
    val getMyPlacesUseCase = GetMyPlacesUseCase(placeRepositoryImpl)
    val getPendingPlacesUseCase = GetPendingPlacesUseCase(placeRepositoryImpl)
    val approvePlaceUseCase = ApprovePlaceUseCase(placeRepositoryImpl)
    val resubmitRejectedPlaceUseCase = ResubmitRejectedPlaceUseCase(placeRepositoryImpl)
    val registerPlaceAsOwnerUseCase = RegisterPlaceAsOwnerUseCase(placeRepositoryImpl)
    val createPlaceUseCase = CreatePlaceUseCase(placeRepositoryImpl)
    val updatePlaceUseCase = UpdatePlaceUseCase(placeRepositoryImpl)
    val getPlaceReviewsUseCase = GetPlaceReviewsUseCase(placeRepositoryImpl)
    val upsertPlaceReviewUseCase = UpsertPlaceReviewUseCase(placeRepositoryImpl)
    val getAvailableBookableSlotsUseCase = GetAvailableBookableSlotsUseCase(bookingRepository)
    val createBookingUseCase = CreateBookingUseCase(bookingRepository)
    val createWeekTimeSlotsUseCase = CreateWeekTimeSlotsUseCase(bookingRepository)
    val loadOwnerBookingScheduleUseCase = LoadOwnerBookingScheduleUseCase(bookingRepository)
    val loadVenueBookingsUseCase = LoadVenueBookingsUseCase(bookingRepository)
    val getMyActiveBookingsUseCase = GetMyActiveBookingsUseCase(bookingRepository)
    val cancelBookingUseCase = CancelBookingUseCase(bookingRepository)
    val addSeatUseCase = AddSeatUseCase(bookingRepository)
    val generateVenueSeatLayoutUseCase = GenerateVenueSeatLayoutUseCase(bookingRepository)
    val listSeatsUseCase = ListSeatsUseCase(bookingRepository)
    val saveCinemaSeatLayoutUseCase = SaveCinemaSeatLayoutUseCase(bookingRepository)
    val addCinemaSessionUseCase = AddCinemaSessionUseCase(bookingRepository)
    val updateCinemaSessionUseCase = UpdateCinemaSessionUseCase(bookingRepository)
    val deleteCinemaSessionUseCase = DeleteCinemaSessionUseCase(bookingRepository)
    val clearCinemaHallUseCase = ClearCinemaHallUseCase(bookingRepository)
    val deleteVenueSeatUseCase = DeleteVenueSeatUseCase(bookingRepository)
    val addCinemaSeatAtCellUseCase = AddCinemaSeatAtCellUseCase(bookingRepository)
    val CheckAuthUseCase = CheckAuthUseCase(Authrepository)
    val SignInUseCase = SignInUseCase(Authrepository)
    val SignUpUseCase = SignUpUseCase(Authrepository)
    val signOutUseCase = SignOutUseCase(Authrepository)
    val getProfileUseCase = GetProfileUseCase(profileRepository)
    val saveProfileNameUseCase = SaveProfileNameUseCase(profileRepository)
    val saveProfilePhoneUseCase = SaveProfilePhoneUseCase(profileRepository)
}