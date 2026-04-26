package com.app.quickfun.ui.place

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.VenueBooking
import com.app.quickfun.ui.place.model.PlaceIntent

@Composable
fun VenueBookingsDialog(placeVm: PlaceViewModel) {
    val placeState by placeVm.state.collectAsState()
    val place = placeState.venueBookingsPlace ?: return

    AlertDialog(
        onDismissRequest = {
            if (!placeState.isLoadingVenueBookings) {
                placeVm.obtainEvent(PlaceIntent.CloseVenueBookings)
            }
        },
        title = { Text("Брони: ${place.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    placeState.isLoadingVenueBookings -> {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                    placeState.venueBookingsError != null -> {
                        Text(
                            placeState.venueBookingsError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    placeState.venueBookings.isEmpty() -> {
                        Text(
                            "Пока нет броней по этому заведению.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        placeState.venueBookings.take(80).forEach { b ->
                            Text(
                                formatVenueBookingLine(b),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = { placeVm.obtainEvent(PlaceIntent.CloseVenueBookings) },
                enabled = !placeState.isLoadingVenueBookings
            ) {
                Text("Закрыть")
            }
        }
    )
}

private fun formatVenueBookingLine(b: VenueBooking): String {
    val time = formatBookingSlotRange(b.startTimeIso, b.endTimeIso)
    val guest = b.guestName?.trim()?.takeIf { it.isNotEmpty() }
        ?: "ID ${b.userId.take(8)}…"
    val st = when (b.status.lowercase()) {
        "active" -> "активна"
        "cancelled" -> "отменена"
        else -> b.status
    }
    return "$time · ${b.displaySeat()} · $guest · $st"
}
