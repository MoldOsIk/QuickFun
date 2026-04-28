package com.app.quickfun.ui.place

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.quickfun.domain.model.BookableSlot
import com.app.quickfun.domain.model.Seat
import com.app.quickfun.domain.model.VenueKind
import com.app.quickfun.domain.model.resolveVenueKind
import com.app.quickfun.ui.place.model.PlaceIntent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingPlaceDialog(placeVm: PlaceViewModel) {
    val placeState by placeVm.state.collectAsState()
    val place = placeState.bookingPlace ?: return
    val kind = place.resolveVenueKind()

    AlertDialog(
        onDismissRequest = {
            if (!placeState.isSubmittingBooking) {
                placeVm.obtainEvent(PlaceIntent.CloseBookingDialog)
            }
        },
        title = { Text("Бронь: ${place.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (placeState.isSubmittingBooking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                when {
                    placeState.isLoadingBookable -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    placeState.bookableSlots.isEmpty() -> {
                        Text(
                            "Нет свободных слотов.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        when {
                            kind == VenueKind.CINEMA ->
                                CinemaBookingContent(
                                    slots = placeState.bookableSlots,
                                    seats = placeState.bookingSeats,
                                    submitting = placeState.isSubmittingBooking,
                                    onBook = { tid, sid ->
                                        placeVm.obtainEvent(
                                            PlaceIntent.SubmitBooking(timeSlotId = tid, seatId = sid)
                                        )
                                    }
                                )

                            kind == VenueKind.BOWLING ||
                                kind == VenueKind.BILLIARDS ||
                                (kind == VenueKind.KARAOKE && placeState.bookingSeats.size > 1) ->
                                ResourceBookingContent(
                                    venueKind = kind,
                                    slots = placeState.bookableSlots,
                                    submitting = placeState.isSubmittingBooking,
                                    onBook = { tid, sid ->
                                        placeVm.obtainEvent(
                                            PlaceIntent.SubmitBooking(timeSlotId = tid, seatId = sid)
                                        )
                                    }
                                )

                            else ->
                                GenericBookingList(
                                    slots = placeState.bookableSlots,
                                    submitting = placeState.isSubmittingBooking,
                                    onBook = { tid, sid ->
                                        placeVm.obtainEvent(
                                            PlaceIntent.SubmitBooking(timeSlotId = tid, seatId = sid)
                                        )
                                    }
                                )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = { placeVm.obtainEvent(PlaceIntent.CloseBookingDialog) },
                enabled = !placeState.isSubmittingBooking
            ) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun GenericBookingList(
    slots: List<BookableSlot>,
    submitting: Boolean,
    onBook: (timeSlotId: Int, seatId: Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(slots, key = { "${it.timeSlotId}-${it.seatId}" }) { slot ->
            BookableSlotRow(
                slot = slot,
                enabled = !submitting,
                onBook = { onBook(slot.timeSlotId, slot.seatId) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceBookingContent(
    venueKind: VenueKind,
    slots: List<BookableSlot>,
    submitting: Boolean,
    onBook: (timeSlotId: Int, seatId: Int) -> Unit
) {
    var timeFirst by remember(slots, venueKind) { mutableStateOf(true) }
    var selectedTimeSlotId by remember(slots, venueKind) { mutableIntStateOf(-1) }
    var selectedSeatId by remember(slots, venueKind) { mutableIntStateOf(-1) }

    Text(
        "Способ бронирования",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = timeFirst,
            onClick = {
                timeFirst = true
                selectedTimeSlotId = -1
                selectedSeatId = -1
            },
            label = { Text("Выбрать время") },
            enabled = !submitting
        )
        FilterChip(
            selected = !timeFirst,
            onClick = {
                timeFirst = false
                selectedTimeSlotId = -1
                selectedSeatId = -1
            },
            label = { Text(venueKind.selectResourceChipLabel()) },
            enabled = !submitting
        )
    }

    Spacer(Modifier.height(12.dp))

    if (timeFirst) {
        val sessions = remember(slots) {
            slots.distinctBy { it.timeSlotId }.sortedBy { it.startTimeIso }
        }
        if (selectedTimeSlotId < 0) {
            Text("Выберите время", style = MaterialTheme.typography.titleSmall)
            sessions.forEach { s ->
                val line = s.displaySession(formatBookingSlotRange(s.startTimeIso, s.endTimeIso))
                OutlinedButton(
                    onClick = { selectedTimeSlotId = s.timeSlotId },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = !submitting
                ) {
                    Text(line, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }
            }
        } else {
            Text(
                "Свободные ${venueKind.resourcePluralNominative()} на это время",
                style = MaterialTheme.typography.titleSmall
            )
            TextButton(
                onClick = { selectedTimeSlotId = -1 },
                enabled = !submitting
            ) {
                Text("← Другое время")
            }
            val forTime = remember(slots, selectedTimeSlotId) {
                slots.filter { it.timeSlotId == selectedTimeSlotId }
                    .sortedWith(compareBy({ it.seatLabel ?: "" }, { it.seatNumber }))
            }
            forTime.forEach { slot ->
                BookableSlotRow(
                    slot = slot,
                    compactSessionLine = true,
                    enabled = !submitting,
                    onBook = { onBook(slot.timeSlotId, slot.seatId) }
                )
            }
        }
    } else {
        val lanes = remember(slots) {
            slots.distinctBy { it.seatId }
                .sortedWith(compareBy({ it.seatLabel ?: "" }, { it.seatNumber }))
        }
        if (selectedSeatId < 0) {
            Text(
                "Выберите ${venueKind.resourceSingularAccusative()}",
                style = MaterialTheme.typography.titleSmall
            )
            lanes.forEach { s ->
                OutlinedButton(
                    onClick = { selectedSeatId = s.seatId },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = !submitting
                ) {
                    Text(s.displaySeat(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }
            }
        } else {
            Text("Свободное время", style = MaterialTheme.typography.titleSmall)
            TextButton(
                onClick = { selectedSeatId = -1 },
                enabled = !submitting
            ) {
                Text(venueKind.pickAnotherResourcePhrase())
            }
            val forSeat = remember(slots, selectedSeatId) {
                slots.filter { it.seatId == selectedSeatId }
                    .sortedBy { it.startTimeIso }
            }
            forSeat.forEach { slot ->
                BookableSlotRow(
                    slot = slot,
                    compactSessionLine = true,
                    enabled = !submitting,
                    onBook = { onBook(slot.timeSlotId, slot.seatId) }
                )
            }
        }
    }
}

private enum class CinemaBookStep {
    Film,
    Time,
    Seat
}

@Composable
private fun CinemaBookingContent(
    slots: List<BookableSlot>,
    seats: List<Seat>,
    submitting: Boolean,
    onBook: (timeSlotId: Int, seatId: Int) -> Unit
) {
    var step by remember(slots) { mutableStateOf(CinemaBookStep.Film) }
    var selectedFilmTitle by remember(slots) { mutableStateOf<String?>(null) }
    var selectedTimeSlotId by remember(slots) { mutableIntStateOf(-1) }

    val distinctSessions = remember(slots) {
        slots.distinctBy { it.timeSlotId }.sortedBy { it.startTimeIso }
    }
    val sessionsByFilm = remember(slots) {
        distinctSessions.groupBy { s ->
            s.sessionLabel?.trim()?.takeIf { it.isNotEmpty() } ?: "Сеанс"
        }
    }

    when (step) {
        CinemaBookStep.Film -> {
            Text("Выберите фильм", style = MaterialTheme.typography.titleSmall)
            sessionsByFilm.keys.sorted().forEach { film ->
                OutlinedButton(
                    onClick = {
                        selectedFilmTitle = film
                        step = CinemaBookStep.Time
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = !submitting
                ) {
                    Text(film, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        CinemaBookStep.Time -> {
            val film = selectedFilmTitle ?: ""
            val showings = sessionsByFilm[film].orEmpty()
            TextButton(
                onClick = {
                    step = CinemaBookStep.Film
                    selectedFilmTitle = null
                },
                enabled = !submitting
            ) {
                Text("← К фильмам")
            }
            Text(film, style = MaterialTheme.typography.titleSmall)
            Text(
                "Выберите время сеанса",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            showings.forEach { s ->
                val timeLine = formatBookingSlotRange(s.startTimeIso, s.endTimeIso)
                OutlinedButton(
                    onClick = {
                        selectedTimeSlotId = s.timeSlotId
                        step = CinemaBookStep.Seat
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = !submitting
                ) {
                    Text(timeLine, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        CinemaBookStep.Seat -> {
            val sessionSlots = remember(slots, selectedTimeSlotId) {
                slots.filter { it.timeSlotId == selectedTimeSlotId }
            }
            val freeIds = remember(sessionSlots) { sessionSlots.map { it.seatId }.toSet() }
            val timeLine = sessionSlots.firstOrNull()?.let { s ->
                formatBookingSlotRange(s.startTimeIso, s.endTimeIso)
            }.orEmpty()
            val filmTitle = selectedFilmTitle ?: ""

            Text(filmTitle, style = MaterialTheme.typography.titleSmall)
            Text(timeLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(
                onClick = {
                    step = CinemaBookStep.Time
                    selectedTimeSlotId = -1
                },
                enabled = !submitting
            ) {
                Text("← Другое время")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Экран",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                textAlign = TextAlign.Center
            )

            if (seats.isEmpty()) {
                Text(
                    "Нет данных о местах зала. Админ должен сгенерировать схему мест.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                val layoutKey = remember(seats) {
                    seats.joinToString(",") { "${it.id}:${it.layoutX}:${it.layoutY}:${it.rowNumber}:${it.seatNumber}" }
                }
                val maxGy = remember(layoutKey) { seats.maxOf { it.effectiveGridY() } }
                val maxGx = remember(layoutKey) { seats.maxOf { it.effectiveGridX() } }
                val cell = CinemaBookingHallCellDp
                val gap = CinemaBookingHallGapDp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            for (gy in 0..maxGy) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(gap),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                ) {
                                    for (gx in 0..maxGx) {
                                        val seat = seats.find {
                                            it.effectiveGridX() == gx && it.effectiveGridY() == gy
                                        }
                                        if (seat != null) {
                                            val free = seat.id in freeIds
                                            CinemaHallBookingSeatCell(
                                                label = seat.rowDotSeatLabel(),
                                                available = free,
                                                submitting = submitting,
                                                onClick = { onBook(selectedTimeSlotId, seat.id) }
                                            )
                                        } else {
                                            Spacer(Modifier.size(cell))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Светлые — свободны, тусклые — заняты. Нажмите на свободное место.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BookableSlotRow(
    slot: BookableSlot,
    compactSessionLine: Boolean = false,
    enabled: Boolean,
    onBook: () -> Unit
) {
    val label = formatBookingSlotRange(slot.startTimeIso, slot.endTimeIso)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (compactSessionLine) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                text = slot.displaySession(label),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = slot.displaySeat(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onBook,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Забронировать")
        }
    }
}

private val CinemaBookingHallCellDp = 30.dp
private val CinemaBookingHallGapDp = 2.dp

@Composable
private fun CinemaHallBookingSeatCell(
    label: String,
    available: Boolean,
    submitting: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(5.dp)
    val cell = CinemaBookingHallCellDp
    val mod = Modifier
        .size(cell)
        .clip(shape)
    if (available) {
        Box(
            modifier = mod
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                    shape = shape
                )
                .clickable(enabled = !submitting) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Box(
            modifier = mod
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun Seat.effectiveGridX(): Int = layoutX ?: (seatNumber - 1).coerceAtLeast(0)

private fun Seat.effectiveGridY(): Int = layoutY ?: (rowNumber - 1).coerceAtLeast(0)

private fun VenueKind.selectResourceChipLabel(): String =
    when (this) {
        VenueKind.BOWLING -> "Выбрать дорожку"
        VenueKind.BILLIARDS -> "Выбрать стол"
        VenueKind.KARAOKE -> "Выбрать комнату"
        else -> "Выбрать место"
    }

private fun VenueKind.resourcePluralNominative(): String =
    when (this) {
        VenueKind.BOWLING -> "дорожки"
        VenueKind.BILLIARDS -> "столы"
        VenueKind.KARAOKE -> "комнаты"
        else -> "места"
    }

private fun VenueKind.resourceSingularAccusative(): String =
    when (this) {
        VenueKind.BOWLING -> "дорожку"
        VenueKind.BILLIARDS -> "стол"
        VenueKind.KARAOKE -> "комнату"
        else -> "место"
    }

private fun VenueKind.pickAnotherResourcePhrase(): String =
    when (this) {
        VenueKind.BOWLING -> "← Другая дорожка"
        VenueKind.BILLIARDS -> "← Другой стол"
        VenueKind.KARAOKE -> "← Другая комната"
        else -> "← Другое место"
    }
