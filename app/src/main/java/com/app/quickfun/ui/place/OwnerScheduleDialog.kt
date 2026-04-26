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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.VenueKind
import com.app.quickfun.domain.model.VenueSeatLayoutConfig
import com.app.quickfun.domain.model.resolveVenueKind
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
    val busy =
        placeState.isGeneratingSlots ||
            placeState.isAddingSeat ||
            placeState.isGeneratingVenueLayout ||
            placeState.isAddingCinemaSession

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
                        placeVm = placeVm,
                        enabled = !busy,
                        isGeneratingLayout = placeState.isGeneratingVenueLayout
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    if (kind == VenueKind.CINEMA) {
                        CinemaSessionForm(
                            placeId = place.id,
                            placeVm = placeVm,
                            enabled = !busy
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
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
                    Text(
                        "Места (${placeState.ownerSeats.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
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
private fun CinemaSessionForm(
    placeId: String,
    placeVm: PlaceViewModel,
    enabled: Boolean
) {
    val placeState by placeVm.state.collectAsState()
    var title by remember(placeId) { mutableStateOf("") }
    var dateYmd by remember(placeId) { mutableStateOf("") }
    var sh by remember(placeId) { mutableStateOf("18") }
    var sm by remember(placeId) { mutableStateOf("0") }
    var eh by remember(placeId) { mutableStateOf("20") }
    var em by remember(placeId) { mutableStateOf("30") }

    Text(
        "Сеанс кино (отдельно от недельной сетки)",
        style = MaterialTheme.typography.titleSmall
    )
    Text(
        "Дата в формате ГГГГ-ММ-ДД, время — локальное на устройстве.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
    )
    OutlinedTextField(
        value = title,
        onValueChange = { title = it.take(120) },
        label = { Text("Фильм / название сеанса") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled && !placeState.isAddingCinemaSession
    )
    OutlinedTextField(
        value = dateYmd,
        onValueChange = { v -> dateYmd = v.filter { c -> c.isDigit() || c == '-' }.take(10) },
        label = { Text("Дата (2026-04-10)") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        singleLine = true,
        enabled = enabled && !placeState.isAddingCinemaSession
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
            enabled = enabled && !placeState.isAddingCinemaSession
        )
        OutlinedTextField(
            value = sm,
            onValueChange = { sm = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Мин н.") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled && !placeState.isAddingCinemaSession
        )
        OutlinedTextField(
            value = eh,
            onValueChange = { eh = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Час к.") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled && !placeState.isAddingCinemaSession
        )
        OutlinedTextField(
            value = em,
            onValueChange = { em = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Мин к.") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled && !placeState.isAddingCinemaSession
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
        enabled = enabled && !placeState.isAddingCinemaSession && title.isNotBlank() && dateYmd.length >= 8
    ) {
        if (placeState.isAddingCinemaSession) {
            CircularProgressIndicator(
                modifier = Modifier.padding(4.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("Добавить сеанс")
        }
    }
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
    placeVm: PlaceViewModel,
    enabled: Boolean,
    isGeneratingLayout: Boolean
) {
    var bowling by remember(place.id) { mutableStateOf("6") }
    var billiard by remember(place.id) { mutableStateOf("4") }
    var cinemaRows by remember(place.id) { mutableStateOf("5") }
    var cinemaSeats by remember(place.id) { mutableStateOf("12") }
    var karaokeRooms by remember(place.id) { mutableStateOf("1") }

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
            VenueKind.CINEMA -> "Каждое кресло = место; прямоугольный зал по рядам."
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
