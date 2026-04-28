package com.app.quickfun.ui.place

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.PlaceReview
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.profile.model.ProfileState
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceReviewsBottomSheet(
    placeVm: PlaceViewModel,
    profile: ProfileState
) {
    val placeState by placeVm.state.collectAsState()
    val placeId = placeState.reviewsPlaceId ?: return
    val place: Place? = placeState.places.find { it.id == placeId }
        ?: placeState.myPlaces.find { it.id == placeId }
    val title = place?.name ?: "Заведение"
    val approved = place?.status?.lowercase() == "approved"
    val isOwner = place?.ownerId != null &&
        profile.userId.isNotEmpty() &&
        place?.ownerId == profile.userId
    val loggedIn = profile.userId.isNotEmpty() && !profile.isLoading
    val myReview = remember(placeState.placeReviews, profile.userId) {
        placeState.placeReviews.find { it.userId == profile.userId }
    }
    var ratingDraft by remember { mutableIntStateOf(8) }
    var bodyDraft by remember { mutableStateOf("") }
    var reviewBodyFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = reviewBodyFocused) {
        focusManager.clearFocus()
    }

    LaunchedEffect(
        placeState.reviewsPlaceId,
        placeState.isLoadingPlaceReviews,
        placeState.placeReviews
    ) {
        if (placeState.isLoadingPlaceReviews) return@LaunchedEffect
        val mine = placeState.placeReviews.find { it.userId == profile.userId }
        if (mine != null) {
            ratingDraft = mine.rating
            bodyDraft = mine.body
        } else {
            ratingDraft = 8
            bodyDraft = ""
        }
    }

    val avg = remember(placeState.placeReviews) {
        if (placeState.placeReviews.isEmpty()) null
        else placeState.placeReviews.map { it.rating.toDouble() }.average()
    }

    ModalBottomSheet(
        onDismissRequest = { placeVm.obtainEvent(PlaceIntent.ClosePlaceReviews) },
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Отзывы: $title",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (avg != null) {
                Text(
                    text = "Средняя оценка: ${"%.1f".format(avg)} / 10 (${placeState.placeReviews.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (!placeState.isLoadingPlaceReviews) {
                Text(
                    text = "Пока нет отзывов.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (loggedIn && approved && !isOwner) {
                Text(
                    text = if (myReview != null) "Ваш отзыв (можно изменить и сохранить снова)" else "Оставить отзыв",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Оценка: $ratingDraft / 10",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = ratingDraft.toFloat(),
                    onValueChange = { ratingDraft = it.toInt().coerceIn(1, 10) },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bodyDraft,
                    onValueChange = { bodyDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { reviewBodyFocused = it.isFocused },
                    label = { Text("Текст отзыва") },
                    minLines = 3,
                    singleLine = false,
                    enabled = !placeState.isSubmittingPlaceReview,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = {
                        placeVm.obtainEvent(
                            PlaceIntent.SubmitPlaceReview(placeId, ratingDraft, bodyDraft)
                        )
                    },
                    enabled = !placeState.isSubmittingPlaceReview && bodyDraft.trim().length >= 3,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (placeState.isSubmittingPlaceReview) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(22.dp)
                                .padding(vertical = 2.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (myReview != null) "Обновить отзыв" else "Отправить отзыв")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            } else if (!loggedIn) {
                Text(
                    text = "Войдите в аккаунт, чтобы оставить отзыв.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else if (isOwner) {
                Text(
                    text = "Владелец не может оставлять отзыв своему заведению.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else if (!approved) {
                Text(
                    text = "Отзывы доступны для одобренных заведений.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text(
                text = "Все отзывы",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            when {
                placeState.isLoadingPlaceReviews && placeState.placeReviews.isEmpty() -> {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(320.dp)
                    ) {
                        items(placeState.placeReviews, key = { it.id }) { r ->
                            ReviewCard(review = r)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { placeVm.obtainEvent(PlaceIntent.ClosePlaceReviews) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
private fun ReviewCard(review: PlaceReview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.authorDisplayName?.takeIf { it.isNotBlank() } ?: "Пользователь",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${review.rating}/10",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = review.body,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
