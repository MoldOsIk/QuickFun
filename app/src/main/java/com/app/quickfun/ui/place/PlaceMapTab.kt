package com.app.quickfun.ui.place

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.Log
import android.content.pm.PackageManager
import android.location.LocationManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.app.quickfun.BuildConfig
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.displayVenueTypeRu
import com.app.quickfun.map.resolveMapPinCoordinates
import com.app.quickfun.ui.place.model.PlaceIntent
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sqrt

private const val DEFAULT_LAT = 55.751244
private const val DEFAULT_LON = 37.618423
private const val DEFAULT_ZOOM = 10f

private const val MAP_DEBUG_TAG = "QuickFunMap"

/** Расстояние по поверхности сферы (метры). */
private fun Context.hasRuntimeLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Камера так, чтобы все метки попали в видимую область (иначе при центре на GPS метки «вне экрана»). */
private fun cameraPositionForAllPins(
    map: Map,
    pins: List<Triple<Place, Double, Double>>
): CameraPosition? {
    if (pins.isEmpty()) return null
    val lats = pins.map { it.second }
    val lons = pins.map { it.third }
    var minLat = lats.minOrNull()!!
    var maxLat = lats.maxOrNull()!!
    var minLon = lons.minOrNull()!!
    var maxLon = lons.maxOrNull()!!
    val minSpan = 0.015
    if (maxLat - minLat < minSpan) {
        val d = (minSpan - (maxLat - minLat)) / 2
        minLat -= d
        maxLat += d
    }
    if (maxLon - minLon < minSpan) {
        val d = (minSpan - (maxLon - minLon)) / 2
        minLon -= d
        maxLon += d
    }
    val box = BoundingBox(Point(minLat, minLon), Point(maxLat, maxLon))
    val geometry = Geometry.fromBoundingBox(box)
    return map.cameraPosition(geometry)
}

/** Радиус «попадания» по тапу на карту (не в сам пин) — зависит от зума. */
private fun maxPickDistanceMeters(zoom: Float): Double = when {
    zoom < 5f -> 120_000.0
    zoom < 8f -> 35_000.0
    zoom < 11f -> 8_000.0
    zoom < 14f -> 2_000.0
    zoom < 17f -> 500.0
    else -> 320.0
}

private fun findNearestPlaceAtTap(
    tapLat: Double,
    tapLon: Double,
    pins: List<Triple<Place, Double, Double>>,
    maxMeters: Double
): Place? {
    if (pins.isEmpty()) return null
    var best: Place? = null
    var bestD = Double.MAX_VALUE
    for ((p, la, lo) in pins) {
        val d = distanceMeters(tapLat, tapLon, la, lo)
        if (d <= maxMeters && d < bestD) {
            bestD = d
            best = p
        }
    }
    return best
}

/** Координаты из БД пригодны для метки (null или 0,0 — нет). */
private fun dbCoordsForPin(lat: Double?, lon: Double?): Pair<Double, Double>? {
    if (lat == null || lon == null) return null
    if (lat == 0.0 && lon == 0.0) return null
    return lat to lon
}

internal fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val p = kotlin.math.PI / 180
    val a =
        0.5 - cos((lat2 - lat1) * p) / 2 +
            cos(lat1 * p) * cos(lat2 * p) * (1 - cos((lon2 - lon1) * p)) / 2
    return 2 * r * asin(sqrt(a))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceMapTab(placeVm: PlaceViewModel) {
    val placeState by placeVm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var mapSheetExpanded by remember { mutableStateOf(false) }
    /** После перехода с каталога по адресу не откатывать камеру в «все метки». */
    var suppressAutoFitAllPins by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPlace?.id) {
        mapSheetExpanded = false
    }
    var userLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }

    val mapView = remember(context) {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // На одно окно карты MapKit допускает только один UserLocationLayer (id _location).
    var userLocationLayerCreated by remember(mapView) { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        locationPermissionGranted =
            granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationPermissionGranted) {
            readLastKnownLocation(context)?.let { userLatLng = it }
        }
    }

    LaunchedEffect(Unit) {
        if (context.hasRuntimeLocationPermission()) {
            locationPermissionGranted = true
            readLastKnownLocation(context)?.let { userLatLng = it }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val geocodeCache = remember { mutableStateMapOf<String, Pair<Double, Double>>() }

    val mapPlaces = remember(placeState.places, placeState.myPlaces) {
        val byId = LinkedHashMap<String, Place>()
        // Сначала «Моё», затем каталог — чтобы полная строка из общего select не затиралась
        // урезанным дублем из myPlaces (иначе city/address=null → геокодинг не запускается).
        for (p in placeState.myPlaces) byId[p.id] = p
        for (p in placeState.places) byId[p.id] = p
        byId.values.toList()
    }

    LaunchedEffect(mapPlaces) {
        val apiKey = BuildConfig.MAPKIT_API_KEY.trim()
        for (p in mapPlaces) {
            val hasDbCoords = dbCoordsForPin(p.latitude, p.longitude) != null
            val hasAddress = !p.city.isNullOrBlank() || !p.address.isNullOrBlank()
            val legacyZeroCoords = p.latitude == 0.0 && p.longitude == 0.0
            if (hasDbCoords || geocodeCache.containsKey(p.id)) continue
            if (!hasAddress && !legacyZeroCoords) {
                Log.d(
                    MAP_DEBUG_TAG,
                    "geocode skip no address id=${p.id} name=${p.name} locId=${p.locationId}"
                )
                continue
            }
            val resolved = resolveMapPinCoordinates(context, apiKey, p.city, p.address, p.name)
            if (resolved != null) {
                geocodeCache[p.id] = resolved
            } else {
                Log.d(
                    MAP_DEBUG_TAG,
                    "geocode miss id=${p.id} name=${p.name} city=${p.city} address=${p.address} " +
                        "locId=${p.locationId}"
                )
            }
        }
    }

    // Нельзя remember(..., geocodeCache): ссылка на map не меняется при новых координатах —
    // метки после геокодинга не появлялись. derivedStateOf подписывается на чтения из geocodeCache.
    val placesWithPins by remember(mapPlaces) {
        derivedStateOf {
            mapPlaces.mapNotNull { p ->
                val pin = dbCoordsForPin(p.latitude, p.longitude) ?: geocodeCache[p.id]
                pin?.let { (lat, lon) -> Triple(p, lat, lon) }
            }
        }
    }

    val pinsLatest = rememberUpdatedState(placesWithPins)
    val onPickPlace = rememberUpdatedState<(Place) -> Unit> { p -> selectedPlace = p }

    LaunchedEffect(mapPlaces, placesWithPins, placeState.isLoading) {
        val all = mapPlaces
        Log.d(
            MAP_DEBUG_TAG,
            "getPlaces: total=${all.size}, pinsOnMap=${placesWithPins.size}, loading=${placeState.isLoading}"
        )
        all.forEachIndexed { i, p ->
            Log.d(
                MAP_DEBUG_TAG,
                "#$i id=${p.id} name=${p.name} locationId=${p.locationId} " +
                    "lat=${p.latitude} lon=${p.longitude} status=${p.status} " +
                    "city=${p.city} addr=${p.address}"
            )
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
        }
    }

    // Без разрешения не создаём слой (SecurityException). Повторно createUserLocationLayer нельзя — краш.
    LaunchedEffect(mapView, locationPermissionGranted) {
        if (!locationPermissionGranted || !context.hasRuntimeLocationPermission()) return@LaunchedEffect
        if (userLocationLayerCreated) return@LaunchedEffect
        val layer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
        layer.isVisible = true
        userLocationLayerCreated = true
    }

    // Тап по пустому месту карты рядом с меткой: открываем ближайшее заведение (не нужно попадать в пиксель иконки).
    DisposableEffect(mapView, placesWithPins) {
        val map = mapView.mapWindow.map
        val inputListener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                val pins = pinsLatest.value
                if (pins.isEmpty()) return
                val lat = point.latitude
                val lon = point.longitude
                if (abs(lat) > 90.0 || abs(lon) > 180.0) return
                val nearest = findNearestPlaceAtTap(
                    lat,
                    lon,
                    pins,
                    maxPickDistanceMeters(map.cameraPosition.zoom)
                ) ?: return
                onPickPlace.value.invoke(nearest)
            }

            override fun onMapLongTap(map: Map, point: Point) = Unit
        }
        map.addInputListener(inputListener)
        onDispose {
            map.removeInputListener(inputListener)
        }
    }

    DisposableEffect(mapView, placesWithPins) {
        val map = mapView.mapWindow.map
        // Якорь: низ по центру — «ножка» пина в точке заведения; scale 1 — размер задаётся bitmap в dp.
        val pinStyle = IconStyle().apply {
            scale = 1f
            anchor = PointF(0.5f, 1f)
        }
        val collection = map.mapObjects.addCollection()
        collection.zIndex = 50f
        for ((place, lat, lon) in placesWithPins) {
            val placemark = collection.addPlacemark(Point(lat, lon))
            placemark.zIndex = 51f
            placemark.setIcon(mapMarkerImageProvider(context, place), pinStyle)
            placemark.addTapListener { _, _ ->
                selectedPlace = place
                true
            }
        }
        onDispose {
            collection.clear()
        }
    }

    // Сначала все заведения в кадр; иначе при включённом GPS карта смотрит на «меня», а метки в других городах не видны.
    LaunchedEffect(mapView, placesWithPins, placeState.pendingMapFocusPlaceId, suppressAutoFitAllPins) {
        val map = mapView.mapWindow.map
        if (placesWithPins.isEmpty()) return@LaunchedEffect
        if (placeState.pendingMapFocusPlaceId != null) return@LaunchedEffect
        if (suppressAutoFitAllPins) return@LaunchedEffect
        val cp = cameraPositionForAllPins(map, placesWithPins) ?: return@LaunchedEffect
        map.move(cp, Animation(Animation.Type.SMOOTH, 0.45f), null)
    }

    LaunchedEffect(
        placeState.pendingMapFocusPlaceId,
        placesWithPins,
        mapPlaces,
        placeState.isLoading,
        mapView
    ) {
        val id = placeState.pendingMapFocusPlaceId ?: return@LaunchedEffect
        val triple = placesWithPins.find { it.first.id == id }
        if (triple != null) {
            val map = mapView.mapWindow.map
            val (_, lat, lon) = triple
            map.move(
                CameraPosition(Point(lat, lon), 16f, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.55f),
                null
            )
            selectedPlace = triple.first
            mapSheetExpanded = false
            suppressAutoFitAllPins = true
            placeVm.obtainEvent(PlaceIntent.ConsumeMapFocusRequest)
            return@LaunchedEffect
        }
        if (!placeState.isLoading && mapPlaces.none { it.id == id }) {
            placeVm.obtainEvent(PlaceIntent.ConsumeMapFocusRequest)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            placeVm.obtainEvent(PlaceIntent.ConsumeMapFocusRequest)
        }
    }

    LaunchedEffect(mapView, userLatLng, placesWithPins) {
        val map = mapView.mapWindow.map
        if (placesWithPins.isNotEmpty()) return@LaunchedEffect
        val targetPoint: Point
        val zoom: Float
        when {
            userLatLng != null -> {
                targetPoint = Point(userLatLng!!.first, userLatLng!!.second)
                zoom = 13f
            }
            else -> {
                targetPoint = Point(DEFAULT_LAT, DEFAULT_LON)
                zoom = DEFAULT_ZOOM
            }
        }
        map.move(
            CameraPosition(targetPoint, zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.35f),
            null
        )
    }

    if (BuildConfig.MAPKIT_API_KEY.trim().isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Укажите ключ MapKit: в local.properties добавьте MAPKIT_API_KEY=ваш_ключ " +
                    "(кабинет разработчика Yandex — MapKit Mobile SDK), затем пересоберите проект.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            if (placeState.isLoading && placeState.places.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }
    }

    selectedPlace?.let { sel ->
        val place = mapPlaces.find { it.id == sel.id } ?: sel
        val mapPinLatLng = dbCoordsForPin(place.latitude, place.longitude)
            ?: geocodeCache[place.id]
        ModalBottomSheet(
            onDismissRequest = { selectedPlace = null },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            MapPlaceInfoSheet(
                place = place,
                mapPinLatLng = mapPinLatLng,
                expanded = mapSheetExpanded,
                onExpandDetail = { mapSheetExpanded = true },
                onCollapseDetail = { mapSheetExpanded = false },
                userLatLng = userLatLng,
                placeVm = placeVm,
                onRecenterMap = {
                    mapPinLatLng?.let { (la, lo) ->
                        mapView.mapWindow.map.move(
                            CameraPosition(Point(la, lo), 17f, 0f, 0f),
                            Animation(Animation.Type.SMOOTH, 0.45f),
                            null
                        )
                    }
                },
                onDismiss = {
                    selectedPlace = null
                    mapSheetExpanded = false
                },
                onBook = {
                    placeVm.obtainEvent(PlaceIntent.LoadBookableSlots(place.id))
                    selectedPlace = null
                    mapSheetExpanded = false
                }
            )
        }
    }
}

@Composable
private fun MapPlaceInfoSheet(
    place: Place,
    mapPinLatLng: Pair<Double, Double>?,
    expanded: Boolean,
    onExpandDetail: () -> Unit,
    onCollapseDetail: () -> Unit,
    userLatLng: Pair<Double, Double>?,
    placeVm: PlaceViewModel,
    onRecenterMap: () -> Unit,
    onDismiss: () -> Unit,
    onBook: () -> Unit
) {
    val approved = place.status?.lowercase() == "approved"
    val distText = remember(place.id, userLatLng, mapPinLatLng) {
        val pin = mapPinLatLng
        if (userLatLng != null && pin != null) {
            val m = distanceMeters(
                userLatLng.first,
                userLatLng.second,
                pin.first,
                pin.second
            )
            when {
                m < 1000 -> "${m.toInt()} м от вас"
                else -> "${"%.1f".format(m / 1000)} км от вас"
            }
        } else {
            null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 20.dp)
    ) {
        if (!expanded) {
            PlaceVenueCoverOnly(place = place, height = 140.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                place.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                place.displayVenueTypeRu(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            distText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = place.description?.takeIf { it.isNotBlank() } ?: "Описание пока не указано.",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            val collapsedAddress = listOfNotNull(
                place.city?.takeIf { it.isNotBlank() },
                place.address?.takeIf { it.isNotBlank() }
            )
                .joinToString(", ")
                .ifBlank {
                    if (mapPinLatLng != null) "Показать на карте"
                    else "Адрес не указан"
                }
            val addressClickable = mapPinLatLng != null
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
                    collapsedAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (addressClickable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.then(
                        if (addressClickable) Modifier.clickable(onClick = onRecenterMap) else Modifier
                    )
                )
            }
            if (!approved) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Бронь недоступна: заведение не одобрено.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onExpandDetail,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Подробнее: фото и полное описание")
            }
            if (approved) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        placeVm.obtainEvent(PlaceIntent.OpenPlaceReviews(place.id))
                        onDismiss()
                    }
                ) {
                    Text("Отзывы")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
                if (approved) {
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onBook) {
                        Text("Забронировать")
                    }
                }
            }
        } else {
            TextButton(onClick = onCollapseDetail) {
                Text("← Кратко")
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PlaceVenueFullDetailContent(
                    place = place,
                    onOpenInAppMap = mapPinLatLng?.let {
                        { onRecenterMap() }
                    }
                )
                if (!approved) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Бронь недоступна: заведение не одобрено.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (approved) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        placeVm.obtainEvent(PlaceIntent.OpenPlaceReviews(place.id))
                        onDismiss()
                    }
                ) {
                    Text("Отзывы")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
                if (approved) {
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onBook) {
                        Text("Забронировать")
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun readLastKnownLocation(context: Context): Pair<Double, Double>? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val fine = try {
        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    } catch (_: Exception) {
        null
    }
    val coarse = try {
        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    } catch (_: Exception) {
        null
    }
    val loc = listOfNotNull(fine, coarse).maxByOrNull { it.time } ?: return null
    return loc.latitude to loc.longitude
}
