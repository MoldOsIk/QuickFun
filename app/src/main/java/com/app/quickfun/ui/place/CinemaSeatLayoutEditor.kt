package com.app.quickfun.ui.place

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.Seat
import com.app.quickfun.domain.model.SeatLayoutPosition
import kotlin.math.max
import kotlin.math.roundToInt

private data class EditableSeat(
    val id: Int,
    val row: Int,
    val seat: Int,
    val shortLabel: String,
    val gx: Int,
    val gy: Int
)

/**
 * Редактор схемы зала: перестановка (долгое нажатие + drag) и режим «места»:
 * пустая клетка — добавить кресло, на месте — удалить.
 */
@Composable
fun CinemaSeatLayoutEditorDialog(
    seats: List<Seat>,
    isSavingLayout: Boolean,
    isAddingSeat: Boolean,
    isDeletingSeat: Boolean,
    onDismiss: () -> Unit,
    onSaveLayout: (List<SeatLayoutPosition>) -> Unit,
    onDeleteSeat: (seatId: Int) -> Unit,
    onAddSeatAtCell: (layoutX: Int, layoutY: Int, row: Int, seat: Int) -> Unit
) {
    val seatsKey = seats.joinToString("|") { "${it.id}:${it.layoutX}:${it.layoutY}:${it.rowNumber}:${it.seatNumber}" }
    val gridItems = remember(seatsKey) {
        mutableStateListOf<EditableSeat>().apply {
            seats.forEach { s ->
                add(
                    EditableSeat(
                        id = s.id,
                        row = s.rowNumber,
                        seat = s.seatNumber,
                        shortLabel = s.rowDotSeatLabel(),
                        gx = s.layoutX ?: (s.seatNumber - 1).coerceAtLeast(0),
                        gy = s.layoutY ?: (s.rowNumber - 1).coerceAtLeast(0)
                    )
                )
            }
        }
    }

    val density = LocalDensity.current
    val cellDp = 40.dp
    val cellPx = remember(density) { with(density) { cellDp.toPx() } }

    var rearrangeMode by remember { mutableStateOf(true) }
    var dragSeatId by remember { mutableStateOf<Int?>(null) }
    var dragAccum by remember { mutableStateOf(Offset.Zero) }

    var addDialogGx by remember { mutableStateOf(0) }
    var addDialogGy by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addRowText by remember { mutableStateOf("1") }
    var addSeatText by remember { mutableStateOf("1") }

    var deleteConfirmId by remember { mutableStateOf<Int?>(null) }

    val maxGx = gridItems.maxOfOrNull { it.gx } ?: 0
    val maxGy = gridItems.maxOfOrNull { it.gy } ?: 0
    val pad = 2
    val cols = max(maxGx + pad + 1, 8)
    val rowsBoard = max(maxGy + pad + 1, 6)
    val boardW = (cols * 40).dp
    val boardH = (rowsBoard * 40).dp

    fun applyMove(movingId: Int, nx: Int, ny: Int) {
        val mi = gridItems.indexOfFirst { it.id == movingId }
        if (mi < 0) return
        val moving = gridItems[mi]
        val oi = gridItems.indexOfFirst { it.id != movingId && it.gx == nx && it.gy == ny }
        if (oi >= 0) {
            val other = gridItems[oi]
            gridItems[oi] = other.copy(gx = moving.gx, gy = moving.gy)
        }
        gridItems[mi] = moving.copy(gx = nx, gy = ny)
    }

    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val busy = isSavingLayout || isAddingSeat || isDeletingSeat

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showAddDialog = false },
            title = { Text("Новое место (клетка ${addDialogGx + 1}, ${addDialogGy + 1})") },
            text = {
                Column {
                    Text(
                        "Ряд и номер — как на билете (например ряд 3, место 12).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = addRowText,
                        onValueChange = { addRowText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Ряд") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = addSeatText,
                        onValueChange = { addSeatText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Место") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = addRowText.toIntOrNull() ?: return@Button
                        val s = addSeatText.toIntOrNull() ?: return@Button
                        onAddSeatAtCell(addDialogGx, addDialogGy, r, s)
                        showAddDialog = false
                    },
                    enabled = !busy
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !busy) {
                    Text("Отмена")
                }
            }
        )
    }

    deleteConfirmId?.let { sid ->
        val label = gridItems.find { it.id == sid }?.shortLabel ?: "место"
        AlertDialog(
            onDismissRequest = { if (!busy) deleteConfirmId = null },
            title = { Text("Удалить $label?") },
            text = { Text("Брони на это место будут удалены из базы.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSeat(sid)
                        deleteConfirmId = null
                    },
                    enabled = !busy
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }, enabled = !busy) {
                    Text("Отмена")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .fillMaxHeight(0.92f),
        title = { Text("Схема зала (кино)") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = rearrangeMode,
                        onClick = { rearrangeMode = true },
                        label = { Text("Переставить") },
                        enabled = !busy
                    )
                    FilterChip(
                        selected = !rearrangeMode,
                        onClick = { rearrangeMode = false },
                        label = { Text("Добавить / удалить") },
                        enabled = !busy
                    )
                }
                Text(
                    if (rearrangeMode) {
                        "Долгое нажатие на место, затем перетащите по сетке. " +
                            "Занятая клетка — обмен местами. Потом «Сохранить схему»."
                    } else {
                        "Нажмите пустую клетку — добавить кресло. Нажмите место — удалить. " +
                            "После добавления/удаления схема в БД обновится сразу."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Экран",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 560.dp)
                ) {
                    val viewportH = maxHeight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(viewportH)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(verticalScroll)
                        ) {
                            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                                Box(
                                    modifier = Modifier
                                        .width(boardW)
                                        .height(boardH)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    for (gy in 0 until rowsBoard) {
                                        for (gx in 0 until cols) {
                                            val item = gridItems.find { it.gx == gx && it.gy == gy }
                                            key("${gx}_$gy", item?.id ?: 0) {
                                                val dragging = item != null && dragSeatId == item.id
                                                val extra = if (dragging) dragAccum else Offset.Zero
                                                if (item != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .offset {
                                                                IntOffset(
                                                                    (item.gx * cellPx + extra.x).roundToInt(),
                                                                    (item.gy * cellPx + extra.y).roundToInt()
                                                                )
                                                            }
                                                            .widthIn(min = 36.dp, max = 52.dp)
                                                            .height(36.dp)
                                                            .then(
                                                                if (rearrangeMode) {
                                                                    Modifier.pointerInput(item.id, cellPx) {
                                                                        detectDragGesturesAfterLongPress(
                                                                            onDragStart = {
                                                                                dragSeatId = item.id
                                                                                dragAccum = Offset.Zero
                                                                            },
                                                                            onDrag = { change, amount ->
                                                                                dragAccum += amount
                                                                                change.consume()
                                                                            },
                                                                            onDragEnd = {
                                                                                val sid =
                                                                                    dragSeatId
                                                                                        ?: return@detectDragGesturesAfterLongPress
                                                                                val dx =
                                                                                    (dragAccum.x / cellPx).roundToInt()
                                                                                val dy =
                                                                                    (dragAccum.y / cellPx).roundToInt()
                                                                                dragSeatId = null
                                                                                dragAccum = Offset.Zero
                                                                                val cur =
                                                                                    gridItems.find { it.id == sid }
                                                                                        ?: return@detectDragGesturesAfterLongPress
                                                                                val nx =
                                                                                    (cur.gx + dx).coerceAtLeast(0)
                                                                                val ny =
                                                                                    (cur.gy + dy).coerceAtLeast(0)
                                                                                applyMove(sid, nx, ny)
                                                                            },
                                                                            onDragCancel = {
                                                                                dragSeatId = null
                                                                                dragAccum = Offset.Zero
                                                                            }
                                                                        )
                                                                    }
                                                                } else {
                                                                    Modifier.clickable(enabled = !busy) {
                                                                        deleteConfirmId = item.id
                                                                    }
                                                                }
                                                            )
                                                            .background(
                                                                MaterialTheme.colorScheme.primaryContainer,
                                                                RoundedCornerShape(6.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            item.shortLabel,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            maxLines = 1
                                                        )
                                                    }
                                                } else if (!rearrangeMode) {
                                                    Box(
                                                        modifier = Modifier
                                                            .offset {
                                                                IntOffset(
                                                                    (gx * cellPx).roundToInt(),
                                                                    (gy * cellPx).roundToInt()
                                                                )
                                                            }
                                                            .size(36.dp)
                                                            .border(
                                                                1.dp,
                                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .clickable(enabled = !busy) {
                                                                addDialogGx = gx
                                                                addDialogGy = gy
                                                                addRowText = "${gy + 1}"
                                                                addSeatText = "${gx + 1}"
                                                                showAddDialog = true
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveLayout(gridItems.map { SeatLayoutPosition(it.id, it.gx, it.gy) })
                },
                enabled = !busy
            ) {
                if (isSavingLayout) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Сохранить схему")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Закрыть")
            }
        }
    )
}
