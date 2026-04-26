package com.app.quickfun.ui.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.quickfun.ui.place.model.PlaceIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceVenueDetailBottomSheet(
    placeId: String?,
    placeVm: PlaceViewModel,
    onDismiss: () -> Unit,
    showBookButton: Boolean
) {
    if (placeId == null) return
    val placeState by placeVm.state.collectAsState()
    val place = placeState.places.find { it.id == placeId }
        ?: placeState.myPlaces.find { it.id == placeId }
    if (place == null) {
        LaunchedEffect(placeId) {
            onDismiss()
        }
        return
    }
    val approved = place.status?.lowercase() == "approved"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PlaceVenueFullDetailContent(place = place)
            if (!approved) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Бронь недоступна: заведение не одобрено.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
                if (showBookButton && approved) {
                    Spacer(Modifier.padding(start = 8.dp))
                    FilledTonalButton(
                        onClick = {
                            placeVm.obtainEvent(PlaceIntent.LoadBookableSlots(place.id))
                            onDismiss()
                        }
                    ) {
                        Text("Забронировать")
                    }
                }
            }
        }
    }
}
