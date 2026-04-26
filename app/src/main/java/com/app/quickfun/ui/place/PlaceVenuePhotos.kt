package com.app.quickfun.ui.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.quickfun.domain.model.Place

/** Только обложка (для списков и краткого просмотра на карте). */
@Composable
fun PlaceVenueCoverOnly(
    place: Place,
    modifier: Modifier = Modifier,
    height: Dp = 112.dp
) {
    if (!place.coverImageUrl.isNullOrBlank()) {
        AsyncImage(
            model = place.coverImageUrl,
            contentDescription = "Обложка ${place.name}",
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Нет фото",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Полное описание + галерея (экран «подробнее»). */
@Composable
fun PlaceVenueFullDetailContent(
    place: Place,
    modifier: Modifier = Modifier,
    heroHeight: Dp = 180.dp,
    galleryThumbSize: Dp = 96.dp
) {
    Column(modifier = modifier) {
        PlaceVenuePhotosReadOnly(
            place = place,
            heroHeight = heroHeight,
            thumbSize = galleryThumbSize
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = place.name,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        place.categoryName?.takeIf { it.isNotBlank() }?.let { cat ->
            Text(
                text = cat,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(12.dp))
        Text(
            text = place.description?.takeIf { it.isNotBlank() } ?: "Описание пока не указано.",
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = listOfNotNull(
                    place.city?.takeIf { it.isNotBlank() },
                    place.address?.takeIf { it.isNotBlank() }
                ).joinToString(", ").ifBlank { "Адрес не указан" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val lat = place.latitude
        val lon = place.longitude
        if (lat != null && lon != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${formatCoord(lat)}, ${formatCoord(lon)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaceVenuePhotosReadOnly(
    place: Place,
    modifier: Modifier = Modifier,
    heroHeight: Dp = 140.dp,
    thumbSize: Dp = 88.dp
) {
    Column(modifier = modifier) {
        if (!place.coverImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = place.coverImageUrl,
                contentDescription = "Обложка ${place.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
        }
        if (place.galleryPhotos.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(place.galleryPhotos, key = { it.id }) { photo ->
                    AsyncImage(
                        model = photo.url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(thumbSize)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceVenuePhotosEditor(
    place: Place,
    mediaBusy: Boolean,
    onPickCover: () -> Unit,
    onPickGallery: () -> Unit,
    onDeleteGalleryPhoto: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val busy = mediaBusy
    Column(modifier = modifier) {
        Text(
            "Фото заведения",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Обложка (карточка и шапка), до 3 фото в галерее.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (!place.coverImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = place.coverImageUrl,
                    contentDescription = "Обложка",
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Нет обложки", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onPickCover,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (place.coverImageUrl.isNullOrBlank()) "Выбрать обложку" else "Сменить обложку")
        }
        Spacer(Modifier.height(16.dp))
        Text("Галерея", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(place.galleryPhotos, key = { it.id }) { photo ->
                Box {
                    AsyncImage(
                        model = photo.url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { onDeleteGalleryPhoto(photo.id) },
                        enabled = !busy,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        if (place.galleryPhotos.size < 3) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPickGallery,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить в галерею (${place.galleryPhotos.size}/3)")
            }
        }
    }
}
