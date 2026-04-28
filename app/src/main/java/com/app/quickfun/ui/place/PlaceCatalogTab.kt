package com.app.quickfun.ui.place

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.app.quickfun.domain.model.Place
import com.app.quickfun.domain.model.VenueKind
import com.app.quickfun.domain.model.displayVenueTypeRu
import com.app.quickfun.domain.model.resolveVenueKind
import com.app.quickfun.domain.model.ruShortType
import com.app.quickfun.ui.place.model.PlaceEffect
import com.app.quickfun.ui.place.model.PlaceIntent
import com.app.quickfun.ui.profile.model.ProfileState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaceCatalogTab(
    placeVm: PlaceViewModel,
    profile: ProfileState,
    snackbarHostState: SnackbarHostState
) {
    val placeState by placeVm.state.collectAsState()
    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var addName by remember { mutableStateOf("") }
    var addDescription by remember { mutableStateOf("") }
    var addCity by remember { mutableStateOf("") }
    var addAddress by remember { mutableStateOf("") }
    var addSelectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var addLatitude by remember { mutableStateOf("") }
    var addLongitude by remember { mutableStateOf("") }

    var editTarget by remember { mutableStateOf<Place?>(null) }
    var catalogDetailPlaceId by remember { mutableStateOf<String?>(null) }
    var catalogSearchQuery by remember { mutableStateOf("") }
    var catalogKindFilter by remember { mutableStateOf<VenueKind?>(null) }
    var catalogCityFilter by remember { mutableStateOf<String?>(null) }
    var catalogMinRating by remember { mutableStateOf<Float?>(null) }
    var catalogMinVisits by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showAddPlaceDialog) {
        if (showAddPlaceDialog) {
            placeVm.obtainEvent(PlaceIntent.LoadVenueCategories)
        }
    }

    val topCatalogPlaceIds = remember(placeState.places) {
        placeState.places
            .asSequence()
            .filter { it.status?.equals("approved", ignoreCase = true) == true }
            // ln(1+0)=0 ⇒ score=0 у всех без броней — иначе «топ» случайно из нулей.
            .filter { it.catalogSortScore > 0.0 }
            .sortedWith(
                compareByDescending<Place> { it.catalogSortScore }
                    .thenByDescending { it.lifetimeBookingCount }
                    .thenByDescending { it.reviewCount }
                    .thenBy { it.name.lowercase() }
            )
            .take(3)
            .map { it.id }
            .toSet()
    }

    val filteredPlaces = remember(
        placeState.places,
        catalogSearchQuery,
        catalogKindFilter,
        catalogCityFilter,
        catalogMinRating,
        catalogMinVisits
    ) {
        val q = catalogSearchQuery.trim().lowercase()
        placeState.places
            .filter { place ->
                catalogPlaceMatchesFilters(
                    place,
                    q,
                    catalogKindFilter,
                    catalogCityFilter,
                    catalogMinRating,
                    catalogMinVisits
                )
            }
            .sortedWith(
                compareByDescending<Place> { it.catalogSortScore }
                    .thenBy { it.name.lowercase() }
            )
    }
    val isSuperAdmin = !profile.isLoading &&
        profile.roles.any { it.equals("admin", ignoreCase = true) }

    fun canEdit(place: Place): Boolean {
        if (isSuperAdmin) return true
        val uid = profile.userId
        if (uid.isEmpty() || place.ownerId == null) return false
        return place.ownerId == uid
    }

    LaunchedEffect(placeVm) {
        placeVm.effects.collect { effect ->
            when (effect) {
                PlaceEffect.AddPlaceSucceeded -> {
                    showAddPlaceDialog = false
                    addName = ""
                    addDescription = ""
                    addCity = ""
                    addAddress = ""
                    addSelectedCategoryId = null
                    addLatitude = ""
                    addLongitude = ""
                }
                PlaceEffect.EditPlaceSucceeded -> Unit
                PlaceEffect.CinemaSeatLayoutSaved -> Unit
                is PlaceEffect.ShowMessage -> Unit
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSuperAdmin) {
            TextButton(
                onClick = { showAddPlaceDialog = true },
                enabled = !placeState.isAddingPlace && !placeState.isEditingPlace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("+ Добавить место (как администратор)")
            }
        }

        when {
            placeState.isLoading && placeState.places.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    item(key = "catalog_discovery") {
                        CatalogDiscoveryPanel(
                            places = placeState.places,
                            query = catalogSearchQuery,
                            onQueryChange = { catalogSearchQuery = it },
                            kindFilter = catalogKindFilter,
                            onKindFilterChange = { catalogKindFilter = it },
                            cityFilter = catalogCityFilter,
                            onCityFilterChange = { catalogCityFilter = it },
                            minRating = catalogMinRating,
                            onMinRatingChange = { catalogMinRating = it },
                            minVisits = catalogMinVisits,
                            onMinVisitsChange = { catalogMinVisits = it },
                            filteredCount = filteredPlaces.size,
                            totalCount = placeState.places.size,
                            onResetFilters = {
                                catalogSearchQuery = ""
                                catalogKindFilter = null
                                catalogCityFilter = null
                                catalogMinRating = null
                                catalogMinVisits = null
                            }
                        )
                    }
                    items(filteredPlaces, key = { it.id }) { place ->
                        CatalogPlaceCard(
                            place = place,
                            canEdit = canEdit(place),
                            rankHighlight = place.id in topCatalogPlaceIds,
                            onEdit = { editTarget = place },
                            onOpenDetail = { catalogDetailPlaceId = place.id },
                            onBook = { placeVm.obtainEvent(PlaceIntent.LoadBookableSlots(place.id)) }
                        )
                    }
                    if (filteredPlaces.isEmpty() && placeState.places.isNotEmpty()) {
                        item(key = "catalog_empty_filter") {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                )
                            ) {
                                Column(
                                    Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Ничего не подошло",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Попробуйте другой запрос или снимите фильтры.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    TextButton(
                                        onClick = {
                                            catalogSearchQuery = ""
                                            catalogKindFilter = null
                                            catalogCityFilter = null
                                            catalogMinRating = null
                                            catalogMinVisits = null
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text("Сбросить всё")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPlaceDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!placeState.isAddingPlace && !placeState.isEditingPlace) {
                    showAddPlaceDialog = false
                }
            },
            title = { Text("Новое место") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Название *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                    )
                    OutlinedTextField(
                        value = addDescription,
                        onValueChange = { addDescription = it },
                        label = { Text("Описание") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                    )
                    OutlinedTextField(
                        value = addCity,
                        onValueChange = { addCity = it },
                        label = { Text("Город") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                    )
                    OutlinedTextField(
                        value = addAddress,
                        onValueChange = { addAddress = it },
                        label = { Text("Адрес") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                    )
                    OutlinedTextField(
                        value = addLatitude,
                        onValueChange = { addLatitude = it },
                        label = { Text("Широта (необяз.)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace,
                        placeholder = { Text("55.7558") }
                    )
                    OutlinedTextField(
                        value = addLongitude,
                        onValueChange = { addLongitude = it },
                        label = { Text("Долгота (необяз.)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace,
                        placeholder = { Text("37.6173") }
                    )
                    VenueCategoryDropdown(
                        categories = placeState.venueCategories,
                        selectedCategoryId = addSelectedCategoryId,
                        onSelectedCategoryIdChange = { addSelectedCategoryId = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        enabled = !placeState.isAddingPlace && !placeState.isEditingPlace,
                        isLoading = placeState.isLoadingVenueCategories
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val city = addCity.trim().takeIf { it.isNotEmpty() }
                        val address = addAddress.trim().takeIf { it.isNotEmpty() }
                        val err = venueFormCoordsAndAddressError(addLatitude, addLongitude, city, address)
                        if (err != null) {
                            scope.launch { snackbarHostState.showSnackbar(err) }
                            return@Button
                        }
                        val p = parseOptionalLatLonStrings(addLatitude, addLongitude)
                        placeVm.obtainEvent(
                            PlaceIntent.AddPlace(
                                name = addName,
                                description = addDescription.trim().takeIf { it.isNotEmpty() },
                                city = city,
                                address = address,
                                categoryId = addSelectedCategoryId,
                                latitude = p.latitude,
                                longitude = p.longitude
                            )
                        )
                    },
                    enabled = !placeState.isAddingPlace && !placeState.isEditingPlace && addName.isNotBlank()
                ) {
                    if (placeState.isAddingPlace) {
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                    } else {
                        Text("Сохранить")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddPlaceDialog = false },
                    enabled = !placeState.isAddingPlace && !placeState.isEditingPlace
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    editTarget?.let { editing ->
        PlaceEditPlaceDialog(
            editTarget = editing,
            onDismiss = { editTarget = null },
            placeVm = placeVm,
            profile = profile,
            snackbarHostState = snackbarHostState
        )
    }

    PlaceVenueDetailBottomSheet(
        placeId = catalogDetailPlaceId,
        placeVm = placeVm,
        onDismiss = { catalogDetailPlaceId = null },
        showBookButton = true
    )
}

private fun catalogPlaceMatchesFilters(
    place: Place,
    queryLower: String,
    kind: VenueKind?,
    city: String?,
    minRating: Float?,
    minVisits: Int?
): Boolean {
    if (kind != null && place.resolveVenueKind() != kind) return false
    if (city != null) {
        val c = place.city?.trim().orEmpty()
        if (!c.equals(city, ignoreCase = true)) return false
    }
    if (minRating != null) {
        // Порог задаёт пользователь по «живой» средней (как в карточке), а не по байесу для сортировки.
        if (place.reviewCount <= 0) return false
        val avg = place.avgReviewRating ?: return false
        if (avg < minRating.toDouble()) return false
    }
    if (minVisits != null && place.lifetimeBookingCount < minVisits) return false
    if (queryLower.isNotEmpty()) {
        val haystack = buildString {
            append(place.name)
            append(' ')
            append(place.description.orEmpty())
            append(' ')
            append(place.city.orEmpty())
            append(' ')
            append(place.address.orEmpty())
            append(' ')
            append(place.displayVenueTypeRu())
            append(' ')
            append(place.categoryName.orEmpty())
        }.lowercase()
        if (!haystack.contains(queryLower)) return false
    }
    return true
}

private val catalogVenueKindChipsOrder = listOf(
    VenueKind.CINEMA,
    VenueKind.BOWLING,
    VenueKind.BILLIARDS,
    VenueKind.KARAOKE,
    VenueKind.GENERIC
)

private val catalogMinRatingChips: List<Pair<Float?, String>> = listOf(
    null to "Любой",
    6f to "от 6",
    7f to "от 7",
    8f to "от 8",
    9f to "от 9"
)

private val catalogMinVisitsChips: List<Pair<Int?, String>> = listOf(
    null to "Любой",
    3 to "3+",
    10 to "10+",
    25 to "25+",
    50 to "50+"
)

private val CatalogTopGoldFrame = Color(0xFFD4AF37)
private val CatalogTopGoldStar = Color(0xFFFFD700)

@Composable
private fun CatalogDiscoveryPanel(
    places: List<Place>,
    query: String,
    onQueryChange: (String) -> Unit,
    kindFilter: VenueKind?,
    onKindFilterChange: (VenueKind?) -> Unit,
    cityFilter: String?,
    onCityFilterChange: (String?) -> Unit,
    minRating: Float?,
    onMinRatingChange: (Float?) -> Unit,
    minVisits: Int?,
    onMinVisitsChange: (Int?) -> Unit,
    filteredCount: Int,
    totalCount: Int,
    onResetFilters: () -> Unit
) {
    var filtersExpanded by remember { mutableStateOf(false) }
    val cities = remember(places) {
        places.mapNotNull { it.city?.trim()?.takeIf { c -> c.isNotEmpty() } }
            .distinct()
            .sortedBy { it.lowercase() }
    }
    val cityRowScroll = rememberScrollState()
    val filtersActive = query.isNotBlank() ||
        kindFilter != null ||
        cityFilter != null ||
        minRating != null ||
        minVisits != null

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Найти место",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "$filteredCount из $totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (filtersActive) {
                    TextButton(onClick = onResetFilters) {
                        Text("Сбросить")
                    }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .onFocusChanged { focus ->
                        if (focus.isFocused) {
                            filtersExpanded = true
                        }
                    },
                singleLine = true,
                placeholder = { Text("Название, город, адрес…") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Очистить")
                            }
                        }
                        IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                            Icon(
                                imageVector = if (filtersExpanded) {
                                    Icons.Outlined.ExpandLess
                                } else {
                                    Icons.Outlined.Tune
                                },
                                contentDescription = if (filtersExpanded) {
                                    "Свернуть фильтры"
                                } else {
                                    "Фильтры"
                                }
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
            if (!filtersExpanded) {
                Text(
                    "Фильтры: тип, город, оценка по отзывам, посещения.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            AnimatedVisibility(
                visible = filtersExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Text(
                        "Тип",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        catalogVenueKindChipsOrder.forEach { kind ->
                            val selected = kindFilter == kind
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onKindFilterChange(if (selected) null else kind)
                                },
                                label = { Text(kind.ruShortType()) }
                            )
                        }
                    }
                    if (cities.isNotEmpty()) {
                        Text(
                            "Город",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(cityRowScroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val allCities = cityFilter == null
                            FilterChip(
                                selected = allCities,
                                onClick = { onCityFilterChange(null) },
                                label = { Text("Все") }
                            )
                            cities.forEach { city ->
                                val sel = cityFilter != null && city.equals(cityFilter, ignoreCase = true)
                                FilterChip(
                                    selected = sel,
                                    onClick = {
                                        onCityFilterChange(if (sel) null else city)
                                    },
                                    label = { Text(city) }
                                )
                            }
                        }
                    }
                    Text(
                        "Средняя оценка от",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        catalogMinRatingChips.forEach { (value, label) ->
                            val selected = when (value) {
                                null -> minRating == null
                                else -> minRating == value
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onMinRatingChange(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Text(
                        "Посещений от",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        catalogMinVisitsChips.forEach { (value, label) ->
                            val selected = when (value) {
                                null -> minVisits == null
                                else -> minVisits == value
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onMinVisitsChange(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                    TextButton(
                        onClick = { filtersExpanded = false },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 6.dp)
                    ) {
                        Text("Свернуть")
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogPlaceCard(
    place: Place,
    canEdit: Boolean,
    rankHighlight: Boolean,
    onEdit: () -> Unit,
    onOpenDetail: () -> Unit,
    onBook: () -> Unit
) {
    val approved = place.status?.lowercase() == "approved"
    val shape = RoundedCornerShape(16.dp)
    val outlineMuted = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val borderStroke = if (rankHighlight) {
        BorderStroke(2.dp, CatalogTopGoldFrame)
    } else {
        BorderStroke(1.dp, outlineMuted)
    }
    val surfaceColor = when {
        !rankHighlight -> MaterialTheme.colorScheme.surface
        isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> Color(0xFFFFFDF7)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.outlinedCardColors(containerColor = surfaceColor),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (rankHighlight) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "В топе каталога",
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp),
                            tint = CatalogTopGoldStar
                        )
                    }
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (canEdit && place.locationId != null) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Изменить")
                    }
                }
            }

            val st = place.status?.lowercase()
            if (st != null && st != "approved") {
                Text(
                    text = when (st) {
                        "pending" -> "На модерации"
                        "rejected" -> "Отклонено"
                        else -> st
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable { onOpenDetail() }
            ) {
                PlaceVenueCoverOnly(place = place, height = 108.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = place.description?.takeIf { it.isNotBlank() }
                        ?: "Нет описания",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append(place.displayVenueTypeRu())
                        append(" · ")
                        append("Посещений: ")
                        append(place.lifetimeBookingCount)
                        if (place.reviewCount > 0 && place.avgReviewRating != null) {
                            append(" · ")
                            append(
                                "%.1f".format(
                                    Locale.forLanguageTag("ru-RU"),
                                    place.avgReviewRating
                                )
                            )
                            append(" (${place.reviewCount})")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${place.city ?: "—"}, ${place.address.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                val lat = place.latitude
                val lon = place.longitude
                if (lat != null && lon != null) {
                    Text(
                        text = "${formatCoord(lat)}, ${formatCoord(lon)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = "Подробнее",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            if (approved) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onBook,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Забронировать")
                }
            }
        }
    }
}
