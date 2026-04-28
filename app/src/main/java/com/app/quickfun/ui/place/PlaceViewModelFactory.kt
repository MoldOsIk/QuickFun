package com.app.quickfun.ui.place

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.quickfun.data.local.MyPlacesModerationStatusStore
import com.app.quickfun.di.AppModule

class PlaceViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(PlaceViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
        return PlaceViewModel(
            application = application,
            myPlacesModerationStatusStore = MyPlacesModerationStatusStore(application.applicationContext),
            placeRepository = AppModule.placeRepository(),
            getPlacesUseCase = AppModule.getPlacesUseCase,
            getMyPlacesUseCase = AppModule.getMyPlacesUseCase,
            getPendingPlacesUseCase = AppModule.getPendingPlacesUseCase,
            approvePlaceUseCase = AppModule.approvePlaceUseCase,
            createPlaceUseCase = AppModule.createPlaceUseCase,
            updatePlaceUseCase = AppModule.updatePlaceUseCase,
            getAvailableBookableSlotsUseCase = AppModule.getAvailableBookableSlotsUseCase,
            createBookingUseCase = AppModule.createBookingUseCase,
            createWeekTimeSlotsUseCase = AppModule.createWeekTimeSlotsUseCase,
            loadOwnerBookingScheduleUseCase = AppModule.loadOwnerBookingScheduleUseCase,
            loadVenueBookingsUseCase = AppModule.loadVenueBookingsUseCase,
            cancelBookingUseCase = AppModule.cancelBookingUseCase,
            addSeatUseCase = AppModule.addSeatUseCase,
            generateVenueSeatLayoutUseCase = AppModule.generateVenueSeatLayoutUseCase,
            listSeatsUseCase = AppModule.listSeatsUseCase,
            saveCinemaSeatLayoutUseCase = AppModule.saveCinemaSeatLayoutUseCase,
            addCinemaSessionUseCase = AppModule.addCinemaSessionUseCase,
            updateCinemaSessionUseCase = AppModule.updateCinemaSessionUseCase,
            deleteCinemaSessionUseCase = AppModule.deleteCinemaSessionUseCase,
            clearCinemaHallUseCase = AppModule.clearCinemaHallUseCase,
            deleteVenueSeatUseCase = AppModule.deleteVenueSeatUseCase,
            addCinemaSeatAtCellUseCase = AppModule.addCinemaSeatAtCellUseCase,
            getVenueCategoriesUseCase = AppModule.getVenueCategoriesUseCase,
            resubmitRejectedPlaceUseCase = AppModule.resubmitRejectedPlaceUseCase,
            getPlaceReviewsUseCase = AppModule.getPlaceReviewsUseCase,
            upsertPlaceReviewUseCase = AppModule.upsertPlaceReviewUseCase
        ) as T
    }
}
