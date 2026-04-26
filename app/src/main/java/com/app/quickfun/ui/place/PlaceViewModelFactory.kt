package com.app.quickfun.ui.place

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
            addSeatUseCase = AppModule.addSeatUseCase,
            generateVenueSeatLayoutUseCase = AppModule.generateVenueSeatLayoutUseCase,
            listSeatsUseCase = AppModule.listSeatsUseCase,
            addCinemaSessionUseCase = AppModule.addCinemaSessionUseCase
        ) as T
    }
}
