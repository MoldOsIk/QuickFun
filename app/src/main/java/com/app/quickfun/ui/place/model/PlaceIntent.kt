package com.app.quickfun.ui.place.model

import android.net.Uri
import com.app.quickfun.domain.model.VenueSeatLayoutConfig

sealed interface PlaceIntent {
    data object Load : PlaceIntent
    data object LoadMyPlaces : PlaceIntent
    data object LoadModeration : PlaceIntent
    data class ApprovePlace(val placeId: String, val approved: Boolean) : PlaceIntent

    data class AddPlace(
        val name: String,
        val description: String?,
        val city: String?,
        val address: String?,
        val categoryId: Int?,
        val latitude: Double?,
        val longitude: Double?
    ) : PlaceIntent

    data class EditPlace(
        val placeId: String,
        val locationId: Int,
        val name: String,
        val description: String?,
        val city: String?,
        val address: String?,
        val categoryId: Int?,
        val latitude: Double?,
        val longitude: Double?,
        val status: String,
        val coverImageUrl: String?
    ) : PlaceIntent

    data class UploadVenueCover(val placeId: String, val uri: Uri) : PlaceIntent
    data class AddVenueGalleryPhoto(val placeId: String, val uri: Uri) : PlaceIntent
    data class DeleteVenueGalleryPhoto(val photoId: Long) : PlaceIntent

    data class LoadBookableSlots(val placeId: String) : PlaceIntent
    data object CloseBookingDialog : PlaceIntent
    data class SubmitBooking(val timeSlotId: Int, val seatId: Int) : PlaceIntent

    data class OpenOwnerSchedule(val placeId: String) : PlaceIntent
    data object CloseOwnerSchedule : PlaceIntent
    data class GenerateWeekSlots(
        val placeId: String,
        val weekdays: Set<Int>,
        val startHour: Int,
        val endHour: Int,
        val slotMinutes: Int
    ) : PlaceIntent
    data class AddVenueSeat(val placeId: String, val row: Int, val seatNum: Int) : PlaceIntent

    data class GenerateVenueSeatLayout(
        val placeId: String,
        val config: VenueSeatLayoutConfig
    ) : PlaceIntent

    /** Сеанс кино: дата и время в локальной зоне устройства, в БД уходит UTC ISO. */
    data class AddCinemaSession(
        val placeId: String,
        val filmTitle: String,
        val dateYmd: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int
    ) : PlaceIntent

    data class OpenVenueBookings(val placeId: String) : PlaceIntent
    data object CloseVenueBookings : PlaceIntent
}
