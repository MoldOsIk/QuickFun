package com.app.quickfun.ui.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.Place
import com.app.quickfun.ui.place.model.PlaceIntent

@Composable
fun ModerationTab(
    placeVm: PlaceViewModel
) {
    val placeState by placeVm.state.collectAsState()
    var rejectTarget by remember { mutableStateOf<Place?>(null) }
    var rejectReasonDraft by remember { mutableStateOf("") }

    rejectTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                if (!placeState.isModerating) {
                    rejectTarget = null
                    rejectReasonDraft = ""
                }
            },
            title = { Text("Отклонить заявку") },
            text = {
                Column {
                    Text(
                        text = "«${target.name}». Укажите причину — владелец увидит её и сможет исправить заявку.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = rejectReasonDraft,
                        onValueChange = { rejectReasonDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Причина отклонения") },
                        minLines = 3,
                        singleLine = false,
                        enabled = !placeState.isModerating
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val reason = rejectReasonDraft.trim()
                        if (reason.isEmpty()) return@TextButton
                        placeVm.obtainEvent(
                            PlaceIntent.ApprovePlace(
                                placeId = target.id,
                                approved = false,
                                rejectionReason = reason
                            )
                        )
                        rejectTarget = null
                        rejectReasonDraft = ""
                    },
                    enabled = !placeState.isModerating && rejectReasonDraft.trim().isNotEmpty()
                ) {
                    Text("Отклонить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        rejectTarget = null
                        rejectReasonDraft = ""
                    },
                    enabled = !placeState.isModerating
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    when {
        placeState.isLoadingModeration && placeState.pendingPlaces.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        placeState.pendingPlaces.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нет заявок на модерацию",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(placeState.pendingPlaces, key = { it.id }) { place ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = place.description ?: "Без описания",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${place.city ?: "—"} · ${place.address ?: ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        placeVm.obtainEvent(
                                            PlaceIntent.ApprovePlace(place.id, approved = true)
                                        )
                                    },
                                    enabled = !placeState.isModerating,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Одобрить")
                                }
                                OutlinedButton(
                                    onClick = {
                                        rejectReasonDraft = ""
                                        rejectTarget = place
                                    },
                                    enabled = !placeState.isModerating,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Отклонить")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
