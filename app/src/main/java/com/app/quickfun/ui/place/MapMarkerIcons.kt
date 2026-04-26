package com.app.quickfun.ui.place

import android.content.Context
import android.graphics.Bitmap
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.app.quickfun.R
import com.app.quickfun.domain.model.Place
import com.yandex.runtime.image.ImageProvider
import java.util.Locale

/** Высота маркера на карте в dp (ширина сохраняет пропорции исходной картинки). */
private const val MARKER_HEIGHT_DP = 40f

private fun markerHeightPx(context: Context): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        MARKER_HEIGHT_DP,
        context.resources.displayMetrics
    ).toInt().coerceIn(32, 96)

internal fun mapMarkerImageProvider(context: Context, place: Place): ImageProvider {
    val resId = mapMarkerDrawableRes(place)
    val drawable =
        ContextCompat.getDrawable(context, resId)
            ?: return ImageProvider.fromResource(context, resId)
    val targetH = markerHeightPx(context)
    val iw = drawable.intrinsicWidth.takeIf { it > 0 } ?: targetH
    val ih = drawable.intrinsicHeight.takeIf { it > 0 } ?: targetH
    val targetW = (targetH * iw.toFloat() / ih.toFloat()).toInt().coerceAtLeast(1)
    val bmp: Bitmap = drawable.toBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    return ImageProvider.fromBitmap(bmp)
}

@DrawableRes
internal fun mapMarkerDrawableRes(place: Place): Int {
    val cat = place.categoryName?.lowercase(Locale.ROOT)?.trim().orEmpty()
    val name = place.name.lowercase(Locale.ROOT)
    val desc = place.description?.lowercase(Locale.ROOT).orEmpty()
    val blob = "$cat $name $desc"
    return when {
        blob.anyKeyword("кино", "кинотеатр", "cinema", "movie", "фильм", "film") ->
            R.drawable.cinema
        blob.anyKeyword("боулинг", "bowling") ->
            R.drawable.bowling
        blob.anyKeyword("караоке", "karaoke", "караок") ->
            R.drawable.karaoke
        blob.anyKeyword("бильярд", "billiard", "pool", "снукер", "snooker") ->
            R.drawable.billiard
        else -> R.drawable.cinema
    }
}

private fun String.anyKeyword(vararg keys: String): Boolean = keys.any { contains(it) }
