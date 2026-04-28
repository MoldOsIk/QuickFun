package com.app.quickfun.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.app.quickfun.data.remote.SupabaseClient
import com.app.quickfun.data.remote.dto.AvailableBookingSlotRowDto
import com.app.quickfun.data.remote.dto.AvailableBookingSlotsRpcParams
import com.app.quickfun.data.remote.dto.BookingInsertDto
import com.app.quickfun.data.remote.dto.CancelBookingRpcParams
import com.app.quickfun.data.remote.dto.MyActiveBookingRowDto
import com.app.quickfun.data.remote.dto.SeatInsertDto
import com.app.quickfun.data.remote.dto.ClearPlaceSeatsRpcParams
import com.app.quickfun.data.remote.dto.OwnerDeleteSeatRpcParams
import com.app.quickfun.data.remote.dto.OwnerDeleteTimeSlotRpcParams
import com.app.quickfun.data.remote.dto.SeatLayoutPatchDto
import com.app.quickfun.data.remote.dto.SeatRowDto
import com.app.quickfun.data.remote.dto.TimeSlotIdRowDto
import com.app.quickfun.data.remote.dto.TimeSlotInsertDto
import com.app.quickfun.data.remote.dto.TimeSlotRowDto
import com.app.quickfun.data.remote.dto.TimeSlotUpdateDto
import com.app.quickfun.data.remote.dto.UserIdNameRowDto
import com.app.quickfun.data.remote.dto.VenueBookingRowDto
import com.app.quickfun.domain.model.BookableSlot
import com.app.quickfun.domain.model.MyActiveBooking
import com.app.quickfun.domain.model.Seat
import com.app.quickfun.domain.model.SeatLayoutPosition
import com.app.quickfun.domain.model.TimeSlot
import com.app.quickfun.domain.model.TimeSlotToInsert
import com.app.quickfun.domain.model.VenueBooking
import com.app.quickfun.domain.repository.BookingRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.result.PostgrestResult
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private suspend inline fun <reified P : Any> postgrestRpc(
    function: String,
    parameters: P
): PostgrestResult = SupabaseClient.client.postgrest.rpc(function, parameters)

class BookingRepositoryImpl : BookingRepository {

    override suspend fun getAvailableSlots(placeId: String): List<BookableSlot> {
        SupabaseClient.client.auth.loadFromStorage()
        val result = postgrestRpc(
            function = "available_booking_slots",
            parameters = AvailableBookingSlotsRpcParams(p_place_id = placeId)
        )
        val rows = jsonParser.decodeFromString(
            ListSerializer(AvailableBookingSlotRowDto.serializer()),
            result.data
        )
        return rows.map {
            BookableSlot(
                timeSlotId = it.time_slot_id,
                seatId = it.seat_id,
                startTimeIso = normalizeTs(it.start_time),
                endTimeIso = normalizeTs(it.end_time),
                rowNumber = it.row_number,
                seatNumber = it.seat_number,
                seatLabel = it.seat_label?.trim()?.takeIf { s -> s.isNotEmpty() },
                sessionLabel = it.session_label?.trim()?.takeIf { s -> s.isNotEmpty() }
            )
        }
    }

    override suspend fun createBooking(timeSlotId: Int, seatId: Int) {
        SupabaseClient.client.auth.loadFromStorage()
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No active session")
        SupabaseClient.client.from("bookings").insert(
            BookingInsertDto(
                user_id = uid,
                time_slot_id = timeSlotId,
                seat_id = seatId,
                status = "active"
            )
        )
    }

    override suspend fun listSeats(placeId: String): List<Seat> {
        SupabaseClient.client.auth.loadFromStorage()
        return SupabaseClient.client
            .from("seats")
            .select(Columns.raw("id, place_id, row_number, seat_number, label, layout_x, layout_y")) {
                filter {
                    eq("place_id", placeId)
                }
            }
            .decodeList<SeatRowDto>()
            .map {
                Seat(
                    id = it.id,
                    placeId = it.place_id,
                    rowNumber = it.row_number,
                    seatNumber = it.seat_number,
                    label = it.label?.trim()?.takeIf { s -> s.isNotEmpty() },
                    layoutX = it.layout_x,
                    layoutY = it.layout_y
                )
            }
            .sortedWith(compareBy({ it.rowNumber }, { it.seatNumber }))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun listTimeSlotsFromNow(placeId: String): List<TimeSlot> {
        SupabaseClient.client.auth.loadFromStorage()
        return SupabaseClient.client
            .from("time_slots")
            .select(Columns.raw("id, place_id, start_time, end_time, label")) {
                filter {
                    eq("place_id", placeId)
                }
            }
            .decodeList<TimeSlotRowDto>()
            .map {
                TimeSlot(
                    id = it.id,
                    placeId = it.place_id,
                    startTimeIso = normalizeTs(it.start_time),
                    endTimeIso = normalizeTs(it.end_time),
                    sessionLabel = it.label?.trim()?.takeIf { s -> s.isNotEmpty() }
                )
            }
            .filter { slotStartUtcIsFuture(it.startTimeIso) }
            .sortedBy { it.startTimeIso }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun slotStartUtcIsFuture(startIso: String): Boolean {
        return try {
            val n = normalizeTs(startIso)
            val ldt = LocalDateTime.parse(n.take(19))
            val instant = ldt.atZone(ZoneOffset.UTC).toInstant()
            instant.isAfter(Instant.now())
        } catch (_: Exception) {
            false
        }
    }

    /** Бронь показываем владельцу, пока интервал слота не закончился (конец в UTC после «сейчас»). */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun slotEndUtcIsAfterNow(endIso: String): Boolean {
        return try {
            val n = normalizeTs(endIso)
            val ldt = LocalDateTime.parse(n.take(19))
            val endInstant = ldt.atZone(ZoneOffset.UTC).toInstant()
            endInstant.isAfter(Instant.now())
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun insertTimeSlots(placeId: String, slots: List<TimeSlotToInsert>) {
        if (slots.isEmpty()) return
        SupabaseClient.client.auth.loadFromStorage()
        val rows = slots.map { slot ->
            TimeSlotInsertDto(
                place_id = placeId,
                start_time = slot.startTimeUtcIso,
                end_time = slot.endTimeUtcIso,
                label = slot.sessionLabel?.trim()?.takeIf { it.isNotEmpty() }
            )
        }
        SupabaseClient.client.from("time_slots").insert(rows)
    }

    override suspend fun insertSeat(
        placeId: String,
        rowNumber: Int,
        seatNumber: Int,
        label: String?,
        layoutX: Int?,
        layoutY: Int?
    ) {
        SupabaseClient.client.auth.loadFromStorage()
        SupabaseClient.client.from("seats").insert(
            SeatInsertDto(
                place_id = placeId,
                row_number = rowNumber,
                seat_number = seatNumber,
                label = label?.trim()?.takeIf { it.isNotEmpty() },
                layout_x = layoutX,
                layout_y = layoutY
            )
        )
    }

    override suspend fun ownerDeleteSeat(seatId: Int) {
        SupabaseClient.client.auth.loadFromStorage()
        postgrestRpc("owner_delete_seat", OwnerDeleteSeatRpcParams(p_seat_id = seatId))
    }

    override suspend fun clearPlaceSeats(placeId: String) {
        SupabaseClient.client.auth.loadFromStorage()
        postgrestRpc("clear_place_seats", ClearPlaceSeatsRpcParams(p_place_id = placeId))
    }

    override suspend fun ownerDeleteTimeSlot(timeSlotId: Int) {
        SupabaseClient.client.auth.loadFromStorage()
        postgrestRpc("owner_delete_time_slot", OwnerDeleteTimeSlotRpcParams(p_slot_id = timeSlotId))
    }

    override suspend fun updateTimeSlot(
        timeSlotId: Int,
        placeId: String,
        startTimeUtcIso: String,
        endTimeUtcIso: String,
        label: String?
    ) {
        SupabaseClient.client.auth.loadFromStorage()
        SupabaseClient.client.from("time_slots").update(
            TimeSlotUpdateDto(
                start_time = startTimeUtcIso.trim(),
                end_time = endTimeUtcIso.trim(),
                label = label?.trim()?.takeIf { it.isNotEmpty() }
            )
        ) {
            filter {
                eq("id", timeSlotId)
                eq("place_id", placeId)
            }
        }
    }

    override suspend fun insertSeatsBatch(placeId: String, seats: List<Triple<Int, Int, String?>>) {
        if (seats.isEmpty()) return
        SupabaseClient.client.auth.loadFromStorage()
        val rows = seats.map { (r, s, lbl) ->
            SeatInsertDto(
                place_id = placeId,
                row_number = r,
                seat_number = s,
                label = lbl?.trim()?.takeIf { it.isNotEmpty() }
            )
        }
        SupabaseClient.client.from("seats").insert(rows)
    }

    override suspend fun updateSeatLayouts(placeId: String, positions: List<SeatLayoutPosition>) {
        if (positions.isEmpty()) return
        SupabaseClient.client.auth.loadFromStorage()
        for (p in positions) {
            SupabaseClient.client.from("seats").update(
                SeatLayoutPatchDto(layout_x = p.layoutX, layout_y = p.layoutY)
            ) {
                filter {
                    eq("id", p.seatId)
                    eq("place_id", placeId)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun listVenueBookings(placeId: String): List<VenueBooking> {
        SupabaseClient.client.auth.loadFromStorage()
        val slotIds = SupabaseClient.client
            .from("time_slots")
            .select(Columns.raw("id")) {
                filter {
                    eq("place_id", placeId)
                }
            }
            .decodeList<TimeSlotIdRowDto>()
            .map { it.id }
        if (slotIds.isEmpty()) return emptyList()
        val rows = SupabaseClient.client
            .from("bookings")
            .select(
                Columns.raw(
                    """
                    id,
                    user_id,
                    status,
                    time_slots(start_time, end_time),
                    seats(row_number, seat_number, label)
                    """.trimIndent()
                )
            ) {
                filter {
                    isIn("time_slot_id", slotIds.map { it })
                }
            }
            .decodeList<VenueBookingRowDto>()
        // Не удаляем строки в БД (статистика позже) — скрываем только в UI владельца.
        val visibleRows = rows.filter { row ->
            slotEndUtcIsAfterNow(normalizeTs(row.time_slots.end_time))
        }
        val guestByUserId = loadGuestContactsForVenueBookings(visibleRows.map { it.user_id })
        return visibleRows
            .map {
                VenueBooking(
                    id = it.id,
                    userId = it.user_id,
                    guestName = guestByUserId[it.user_id]?.name,
                    guestPhoneE164 = guestByUserId[it.user_id]?.phoneE164,
                    startTimeIso = normalizeTs(it.time_slots.start_time),
                    endTimeIso = normalizeTs(it.time_slots.end_time),
                    rowNumber = it.seats.row_number,
                    seatNumber = it.seats.seat_number,
                    seatLabel = it.seats.label?.trim()?.takeIf { s -> s.isNotEmpty() },
                    status = it.status
                )
            }
            .sortedWith(
                compareBy<VenueBooking>({ it.startTimeIso }, { it.id })
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun listMyActiveBookings(): List<MyActiveBooking> {
        SupabaseClient.client.auth.loadFromStorage()
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No active session")
        val rows = SupabaseClient.client
            .from("bookings")
            .select(
                Columns.raw(
                    """
                    id,
                    status,
                    time_slots(place_id, start_time, end_time, places(name)),
                    seats(row_number, seat_number, label)
                    """.trimIndent()
                )
            ) {
                filter {
                    eq("user_id", uid)
                    eq("status", "active")
                }
            }
            .decodeList<MyActiveBookingRowDto>()
        return rows
            .filter { row ->
                slotEndUtcIsAfterNow(normalizeTs(row.time_slots.end_time))
            }
            .map { row ->
                MyActiveBooking(
                    bookingId = row.id,
                    placeId = row.time_slots.place_id,
                    placeName = row.time_slots.places?.name?.trim()?.takeIf { it.isNotEmpty() }
                        ?: "Заведение",
                    startTimeIso = normalizeTs(row.time_slots.start_time),
                    endTimeIso = normalizeTs(row.time_slots.end_time),
                    rowNumber = row.seats.row_number,
                    seatNumber = row.seats.seat_number,
                    seatLabel = row.seats.label?.trim()?.takeIf { s -> s.isNotEmpty() },
                    status = row.status
                )
            }
            .sortedWith(compareBy({ it.startTimeIso }, { it.bookingId }))
    }

    override suspend fun cancelBooking(bookingId: Int) {
        SupabaseClient.client.auth.loadFromStorage()
        postgrestRpc("cancel_booking", CancelBookingRpcParams(p_booking_id = bookingId))
    }

    private data class GuestContact(
        val name: String?,
        val phoneE164: String?
    )

    /**
     * Имя и телефон в [public.users]: FK брони ведёт в auth.users, embed в PostgREST к [public.users] не строится.
     */
    private suspend fun loadGuestContactsForVenueBookings(userIds: List<String>): Map<String, GuestContact> {
        val distinct = userIds.distinct()
        if (distinct.isEmpty()) return emptyMap()
        return try {
            SupabaseClient.client
                .from("users")
                .select(Columns.raw("id, name, phone_e164")) {
                    filter {
                        isIn("id", distinct)
                    }
                }
                .decodeList<UserIdNameRowDto>()
                .associate { row ->
                    row.id to GuestContact(
                        name = row.name?.trim()?.takeIf { it.isNotEmpty() },
                        phoneE164 = row.phone_e164?.trim()?.takeIf { it.isNotEmpty() }
                    )
                }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** PostgREST иногда отдаёт время с пробелом вместо T — приводим к ISO-подобной строке для парсинга в UI. */
    private fun normalizeTs(raw: String): String {
        val t = raw.trim()
        return if ('T' !in t && t.length >= 16) {
            t.replaceFirst(" ", "T")
        } else {
            t
        }
    }
}
