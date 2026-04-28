package com.app.quickfun.ui.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.TimeSlot
import com.app.quickfun.domain.model.VenueKind
import com.app.quickfun.domain.model.VenueSeatLayoutConfig
import com.app.quickfun.domain.model.resolveVenueKind
import com.app.quickfun.ui.place.model.PlaceEffect
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.place.model.PlaceState

private val weekdayLabels = listOf(
    1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт",
    5 to "Пт", 6 to "Сб", 7 to "Вс"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerScheduleDialog(placeVm: PlaceViewModel) {
    val placeState by placeVm.state.collectAsState()
    val place = placeState.ownerSchedulePlace ?: return
    val kind = place.resolveVenueKind()
    var showLayoutEditor by remember(place.id) { mutableStateOf(false) }
    LaunchedEffect(placeVm, place.id) {
        placeVm.effects.collect { effect ->
            if (effect == PlaceEffect.CinemaSeatLayoutSaved) {
                showLayoutEditor = false
            }
        }
    }
    LaunchedEffect(placeState.ownerSeats.isEmpty(), kind) {
        if (kind == VenueKind.CINEMA && placeState.ownerSeats.isEmpty()) {
            showLayoutEditor = false
        }
    }
    val busy =
        placeState.isGeneratingSlots ||
            placeState.isAddingSeat ||
            placeState.isGeneratingVenueLayout ||
            placeState.isSavingSeatLayout ||
            placeState.isAddingCinemaSession ||
            placeState.isUpdatingCinemaSession ||
            placeState.isDeletingCinemaSession ||
            placeState.isClearingCinemaHall ||
            placeState.isDeletingVenueSeat

    AlertDialog(
        onDismissRequest = {
            if (!busy) {
                placeVm.obtainEvent(PlaceIntent.CloseOwnerSchedule)
            }
        },
        title = { Text("Настройка: ${place.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (placeState.isLoadingOwnerSchedule) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else {
                    Text(
                        venueKindTitle(kind),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        venueKindSubtitle(kind, place),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    VenueSeatLayoutBlock(
                        place = place,
                        kind = kind,
                        seatCount = placeState.ownerSeats.size,
                        placeVm = placeVm,
                        enabled = !busy,
                        isGeneratingLayout = placeState.isGeneratingVenueLayout
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    if (kind == VenueKind.CINEMA) {
                        CinemaFilmsAdminBlock(
                            placeId = place.id,
                            placeState = placeState,
                            placeVm = placeVm,
                            enabled = !busy
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    if (kind != VenueKind.CINEMA) {
                        Text(
                            "Расписание (слоты на ближайшие 8 дней)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Дни недели:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        WeekdayAndSlotsBlock(place = place, placeState = placeState, placeVm = placeVm)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Text(
                        "Места (${placeState.ownerSeats.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (kind == VenueKind.CINEMA && placeState.ownerSeats.isNotEmpty()) {
                        Button(
                            onClick = { showLayoutEditor = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            enabled = !busy
                        ) {
                            Text("Схема зала (перетащить места)")
                        }
                    }
                    if (placeState.ownerSeats.isEmpty()) {
                        Text(
                            "Пока нет мест — создайте схему выше или добавьте вручную.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    } else {
                        placeState.ownerSeats.take(20).forEach { s ->
                            Text(
                                "· ${s.displayLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        if (placeState.ownerSeats.size > 20) {
                            Text(
                                "… и ещё ${placeState.ownerSeats.size - 20}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (kind == VenueKind.GENERIC) {
                        Text(
                            "Ручное добавление (ряд и номер)",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        ManualSeatBlock(place = place, placeState = placeState, placeVm = placeVm)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = { placeVm.obtainEvent(PlaceIntent.CloseOwnerSchedule) },
                enabled = !busy
            ) {
                Text("Закрыть")
            }
        }
    )

    if (showLayoutEditor && placeState.ownerSeats.isNotEmpty()) {
        CinemaSeatLayoutEditorDialog(
            seats = placeState.ownerSeats,
            isSavingLayout = placeState.isSavingSeatLayout,
            isAddingSeat = placeState.isAddingSeat,
            isDeletingSeat = placeState.isDeletingVenueSeat,
            onDismiss = {
                if (!placeState.isSavingSeatLayout &&
                    !placeState.isAddingSeat &&
                    !placeState.isDeletingVenueSeat
                ) {
                    showLayoutEditor = false
                }
            },
            onSaveLayout = { positions ->
                placeVm.obtainEvent(
                    PlaceIntent.SaveCinemaSeatLayout(place.id, positions)
                )
            },
            onDeleteSeat = { seatId ->
                placeVm.obtainEvent(PlaceIntent.DeleteVenueSeat(place.id, seatId))
            },
            onAddSeatAtCell = { lx, ly, row, seat ->
                placeVm.obtainEvent(
                    PlaceIntent.AddCinemaSeatAtCell(place.id, row, seat, lx, ly)
                )
            }
        )
    }
}

@Composable
private fun WeekdayAndSlotsBlock(
    place: Place,
    placeState: PlaceState,
    placeVm: PlaceViewModel
) {
    var weekdays by remember(place.id) {
        mutableStateOf(setOf(1, 2, 3, 4, 5))
    }
    var startHour by remember(place.id) { mutableStateOf("10") }
    var endHour by remember(place.id) { mutableStateOf("18") }
    var slotMinutes by remember(place.id) { mutableStateOf("60") }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        weekdayLabels.forEach { (dow, label) ->
            val sel = dow in weekdays
            FilterChip(
                selected = sel,
                onClick = {
                    weekdays = if (sel) weekdays - dow else weekdays + dow
                },
                label = { Text(label) },
                enabled = !placeState.isGeneratingSlots
            )
        }
    }
    OutlinedTextField(
        value = startHour,
        onValueChange = { startHour = it.filter { c -> c.isDigit() }.take(2) },
        label = { Text("Час начала (0–22)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !placeState.isGeneratingSlots
    )
    OutlinedTextField(
        value = endHour,
        onValueChange = { endHour = it.filter { c -> c.isDigit() }.take(2) },
        label = { Text("Час окончания (1–23, > начала)") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        singleLine = true,
        enabled = !placeState.isGeneratingSlots
    )
    OutlinedTextField(
        value = slotMinutes,
        onValueChange = {
            slotMinutes = it.filter { c -> c.isDigit() }.take(3)
        },
        label = { Text("Длительность слота (мин), 15–120") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        singleLine = true,
        enabled = !placeState.isGeneratingSlots
    )
    Button(
        onClick = {
            val sh = startHour.toIntOrNull() ?: return@Button
            val eh = endHour.toIntOrNull() ?: return@Button
            val sm = slotMinutes.toIntOrNull() ?: return@Button
            placeVm.obtainEvent(
                PlaceIntent.GenerateWeekSlots(
                    placeId = place.id,
                    weekdays = weekdays,
                    startHour = sh,
                    endHour = eh,
                    slotMinutes = sm
                )
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        enabled = !placeState.isGeneratingSlots && weekdays.isNotEmpty()
    ) {
        if (placeState.isGeneratingSlots) {
            CircularProgressIndicator(
                modifier = Modifier.padding(4.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("Создать слоты на неделю")
        }
    }
    Text(
        "Ближайшие слоты:",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(top = 16.dp)
    )
    val slots = placeState.ownerTimeSlots.take(40)
    if (slots.isEmpty()) {
        Text(
            "Пока нет будущих слотов.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        slots.forEach { ts ->
            val timeLine = formatBookingSlotRange(ts.startTimeIso, ts.endTimeIso)
            val line = ts.sessionLabel?.trim()?.takeIf { it.isNotEmpty() }?.let { t -> "$t · $timeLine" }
                ?: timeLine
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CinemaFilmsAdminBlock(
    placeId: String,
    placeState: PlaceState,
    placeVm: PlaceViewModel,
    enabled: Boolean
) {
    val placeVmState by placeVm.state.collectAsState()
    var editingSlot by remember(placeId) { mutableStateOf<TimeSlot?>(null) }
    var deletingSlot by remember(placeId) { mutableStateOf<TimeSlot?>(null) }
    var title by remember(placeId) { mutableStateOf("") }
    var dateYmd by remember(placeId) { mutableStateOf("") }
    var sh by remember(placeId) { mutableStateOf("18") }
    var sm by remember(placeId) { mutableStateOf("0") }
    var eh by remember(placeId) { mutableStateOf("20") }
    var em by remember(placeId) { mutableStateOf("30") }

    val sessionBusy = placeVmState.isAddingCinemaSession ||
        placeVmState.isUpdatingCinemaSession ||
        placeVmState.isDeletingCinemaSession

    Text(
        "Сеансы (фильмы)",
        style = MaterialTheme.typography.titleSmall
    )
    Text(
        "Для кино расписание только из сеансов: время и длительность задаёте ниже. " +
            "Один фильм можно поставить несколько раз в день.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
    )

    val slots = placeState.ownerTimeSlots
    if (slots.isEmpty()) {
        Text(
            "Пока нет будущих сеансов.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        slots.forEach { ts ->
            val timeLine = formatBookingSlotRange(ts.startTimeIso, ts.endTimeIso)
            val film = ts.sessionLabel?.trim()?.takeIf { it.isNotEmpty() } ?: "Сеанс"
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(film, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        timeLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { editingSlot = ts },
                    enabled = enabled && !sessionBusy
                ) {
                    Text("Изменить")
                }
                TextButton(
                    onClick = { deletingSlot = ts },
                    enabled = enabled && !sessionBusy,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
    Text(
        "Добавить сеанс",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    OutlinedTextField(
        value = title,
        onValueChange = { title = it.take(120) },
        label = { Text("Фильм / название") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled && !sessionBusy
    )
    OutlinedTextField(
        value = dateYmd,
        onValueChange = { v -> dateYmd = v.filter { c -> c.isDigit() || c == '-' }.take(10) },
        label = { Text("Дата (ГГГГ-ММ-ДД)") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        singleLine = true,
        enabled = enabled && !sessionBusy
    )
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = sh,
            onValueChange = { sh = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Час н.") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled && !sessionBusy
        )
        OutlinedTextField(
            value = sm,
            onValueChange = { sm = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Мин н.") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled && !sessionBusy
        )
        OutlinedTextField(
            value = eh,
            onValueChange = { eh = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Час к.") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled && !sessionBusy
        )
        OutlinedTextField(
            value = em,
            onValueChange = { em = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Мин к.") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled && !sessionBusy
        )
    }
    Button(
        onClick = {
            val h0 = sh.toIntOrNull() ?: return@Button
            val m0 = sm.toIntOrNull() ?: return@Button
            val h1 = eh.toIntOrNull() ?: return@Button
            val m1 = em.toIntOrNull() ?: return@Button
            placeVm.obtainEvent(
                PlaceIntent.AddCinemaSession(
                    placeId = placeId,
                    filmTitle = title,
                    dateYmd = dateYmd,
                    startHour = h0,
                    startMinute = m0,
                    endHour = h1,
                    endMinute = m1
                )
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        enabled = enabled && !sessionBusy && title.isNotBlank() && dateYmd.length >= 8
    ) {
        if (placeVmState.isAddingCinemaSession) {
            CircularProgressIndicator(
                modifier = Modifier.padding(4.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("Добавить сеанс")
        }
    }

    editingSlot?.let { slot ->
        CinemaSessionEditDialog(
            slot = slot,
            placeId = placeId,
            placeVm = placeVm,
            onDismiss = { editingSlot = null }
        )
    }

    deletingSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { if (!sessionBusy) deletingSlot = null },
            title = { Text("Удалить сеанс?") },
            text = {
                Text(
                    "${slot.sessionLabel?.trim()?.takeIf { it.isNotEmpty() } ?: "Сеанс"} — " +
                        formatBookingSlotRange(slot.startTimeIso, slot.endTimeIso)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        placeVm.obtainEvent(PlaceIntent.DeleteCinemaSession(placeId, slot.id))
                        deletingSlot = null
                    },
                    enabled = !sessionBusy
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSlot = null }, enabled = !sessionBusy) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun CinemaSessionEditDialog(
    slot: TimeSlot,
    placeId: String,
    placeVm: PlaceViewModel,
    onDismiss: () -> Unit
) {
    val placeState by placeVm.state.collectAsState()
    val (sh0, sm0) = timeSlotStartLocalHourMinute(slot)
    val (eh0, em0) = timeSlotEndLocalHourMinute(slot)
    var title by remember(slot.id) { mutableStateOf(slot.sessionLabel?.trim().orEmpty()) }
    var dateYmd by remember(slot.id) { mutableStateOf(timeSlotStartLocalDateYmd(slot)) }
    var sh by remember(slot.id) { mutableStateOf(sh0.toString()) }
    var sm by remember(slot.id) { mutableStateOf(sm0.toString()) }
    var eh by remember(slot.id) { mutableStateOf(eh0.toString()) }
    var em by remember(slot.id) { mutableStateOf(em0.toString()) }

    val busy = placeState.isUpdatingCinemaSession

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Изменить сеанс") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Фильм / название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !busy
                )
                OutlinedTextField(
                    value = dateYmd,
                    onValueChange = { v -> dateYmd = v.filter { c -> c.isDigit() || c == '-' }.take(10) },
                    label = { Text("Дата") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    enabled = !busy
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sh,
                        onValueChange = { sh = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Час н.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !busy
                    )
                    OutlinedTextField(
                        value = sm,
                        onValueChange = { sm = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Мин н.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !busy
                    )
                    OutlinedTextField(
                        value = eh,
                        onValueChange = { eh = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Час к.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !busy
                    )
                    OutlinedTextField(
                        value = em,
                        onValueChange = { em = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Мин к.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !busy
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h0 = sh.toIntOrNull() ?: return@Button
                    val m0 = sm.toIntOrNull() ?: return@Button
                    val h1 = eh.toIntOrNull() ?: return@Button
                    val m1 = em.toIntOrNull() ?: return@Button
                    placeVm.obtainEvent(
                        PlaceIntent.UpdateCinemaSession(
                            placeId = placeId,
                            timeSlotId = slot.id,
                            filmTitle = title,
                            dateYmd = dateYmd,
                            startHour = h0,
                            startMinute = m0,
                            endHour = h1,
                            endMinute = m1
                        )
                    )
                    onDismiss()
                },
                enabled = !busy && title.isNotBlank() && dateYmd.length >= 8
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Сохранить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun ManualSeatBlock(
    place: Place,
    placeState: PlaceState,
    placeVm: PlaceViewModel
) {
    var newRow by remember(place.id) { mutableStateOf("1") }
    var newSeat by remember(place.id) { mutableStateOf("2") }
    RowFields(
        newRow = newRow,
        newSeat = newSeat,
        onRow = { newRow = it },
        onSeat = { newSeat = it },
        enabled = !placeState.isAddingSeat
    )
    Button(
        onClick = {
            val r = newRow.toIntOrNull() ?: return@Button
            val s = newSeat.toIntOrNull() ?: return@Button
            placeVm.obtainEvent(
                PlaceIntent.AddVenueSeat(place.id, r, s)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        enabled = !placeState.isAddingSeat
    ) {
        if (placeState.isAddingSeat) {
            CircularProgressIndicator(
                modifier = Modifier.padding(4.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("Добавить место")
        }
    }
}

@Composable
private fun VenueSeatLayoutBlock(
    place: Place,
    kind: VenueKind,
    seatCount: Int,
    placeVm: PlaceViewModel,
    enabled: Boolean,
    isGeneratingLayout: Boolean
) {
    var bowling by remember(place.id) { mutableStateOf("6") }
    var billiard by remember(place.id) { mutableStateOf("4") }
    var cinemaRows by remember(place.id) { mutableStateOf("5") }
    var cinemaSeats by remember(place.id) { mutableStateOf("12") }
    var karaokeRooms by remember(place.id) { mutableStateOf("1") }
    var showClearHallConfirm by remember(place.id) { mutableStateOf(false) }

    if (isGeneratingLayout) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
    Text(
        "Схема мест (один раз, пока таблица seats пустая для этого заведения)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    when (kind) {
        VenueKind.BOWLING -> {
            OutlinedTextField(
                value = bowling,
                onValueChange = { bowling = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Сколько дорожек") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                enabled = enabled
            )
            Button(
                onClick = {
                    val n = bowling.toIntOrNull() ?: return@Button
                    placeVm.obtainEvent(
                        PlaceIntent.GenerateVenueSeatLayout(
                            place.id,
                            VenueSeatLayoutConfig.BowlingLanes(n)
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = enabled
            ) {
                Text("Создать дорожки")
            }
        }
        VenueKind.BILLIARDS -> {
            OutlinedTextField(
                value = billiard,
                onValueChange = { billiard = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Сколько столов") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                enabled = enabled
            )
            Button(
                onClick = {
                    val n = billiard.toIntOrNull() ?: return@Button
                    placeVm.obtainEvent(
                        PlaceIntent.GenerateVenueSeatLayout(
                            place.id,
                            VenueSeatLayoutConfig.BilliardTables(n)
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = enabled
            ) {
                Text("Создать столы")
            }
        }
        VenueKind.CINEMA -> {
            if (seatCount > 0) {
                Text(
                    "Зал уже создан: $seatCount мест. Схему можно менять в редакторе ниже.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedButton(
                    onClick = { showClearHallConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить весь зал и все места из базы")
                }
                if (showClearHallConfirm) {
                    AlertDialog(
                        onDismissRequest = { showClearHallConfirm = false },
                        title = { Text("Удалить зал?") },
                        text = {
                            Text(
                                "Будут удалены все $seatCount мест и связанные с ними брони. " +
                                    "Сеансы (слоты) останутся — при необходимости удалите их вручную выше."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    placeVm.obtainEvent(PlaceIntent.ClearCinemaHall(place.id))
                                    showClearHallConfirm = false
                                },
                                enabled = enabled
                            ) {
                                Text("Удалить")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearHallConfirm = false }) {
                                Text("Отмена")
                            }
                        }
                    )
                }
            } else {
                OutlinedTextField(
                    value = cinemaRows,
                    onValueChange = { cinemaRows = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Рядов") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    enabled = enabled
                )
                OutlinedTextField(
                    value = cinemaSeats,
                    onValueChange = { cinemaSeats = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Мест в ряду") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    enabled = enabled
                )
                Button(
                    onClick = {
                        val r = cinemaRows.toIntOrNull() ?: return@Button
                        val s = cinemaSeats.toIntOrNull() ?: return@Button
                        placeVm.obtainEvent(
                            PlaceIntent.GenerateVenueSeatLayout(
                                place.id,
                                VenueSeatLayoutConfig.CinemaGrid(r, s)
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    enabled = enabled
                ) {
                    Text("Сгенерировать зал")
                }
            }
        }
        VenueKind.KARAOKE -> {
            OutlinedTextField(
                value = karaokeRooms,
                onValueChange = { karaokeRooms = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Комнат (1 — одно целое караоке)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                enabled = enabled
            )
            Button(
                onClick = {
                    val n = karaokeRooms.toIntOrNull() ?: return@Button
                    placeVm.obtainEvent(
                        PlaceIntent.GenerateVenueSeatLayout(
                            place.id,
                            VenueSeatLayoutConfig.KaraokeRooms(n)
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = enabled
            ) {
                Text("Создать комнаты")
            }
        }
        VenueKind.GENERIC -> {
            Text(
                "Для этой категории схема не задаётся автоматически — добавьте места вручную ниже.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun venueKindTitle(kind: VenueKind): String =
    when (kind) {
        VenueKind.BOWLING -> "Тип: боулинг"
        VenueKind.BILLIARDS -> "Тип: бильярд"
        VenueKind.CINEMA -> "Тип: кино"
        VenueKind.KARAOKE -> "Тип: караоке"
        VenueKind.GENERIC -> "Тип: универсальный"
    }

private fun venueKindSubtitle(kind: VenueKind, place: Place): String {
    val cat = place.categoryName?.trim()?.takeIf { it.isNotEmpty() } ?: "категория не указана"
    val id = place.categoryId?.let { "id категории: $it" } ?: "id категории: —"
    val hint =
        when (kind) {
            VenueKind.BOWLING -> "Дорожки = отдельные места; одно расписание на зал."
            VenueKind.BILLIARDS -> "Столы = отдельные места; одно расписание на зал."
            VenueKind.CINEMA ->
                "Кино: сеансы задаются отдельно; зал — места с координатами на схеме. Недельная сетка слотов не используется."
            VenueKind.KARAOKE -> "Комнаты = места; при одной комнате — бронь всего караоке по слоту."
            VenueKind.GENERIC ->
                "Точный тип можно задать id в VenueCategoryIdSets (domain) или по имени категории в БД."
        }
    return "$cat · $id. $hint"
}

@Composable
private fun RowFields(
    newRow: String,
    newSeat: String,
    onRow: (String) -> Unit,
    onSeat: (String) -> Unit,
    enabled: Boolean
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = newRow,
            onValueChange = { onRow(it.filter { c -> c.isDigit() }.take(4)) },
            label = { Text("Ряд") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled
        )
        OutlinedTextField(
            value = newSeat,
            onValueChange = { onSeat(it.filter { c -> c.isDigit() }.take(4)) },
            label = { Text("Номер") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled
        )
    }
}
