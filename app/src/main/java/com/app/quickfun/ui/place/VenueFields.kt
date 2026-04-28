package com.app.quickfun.ui.place

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.model.VenueCategory

@Composable
fun VenueCategoryDropdown(
    categories: List<VenueCategory>,
    selectedCategoryId: Int?,
    onSelectedCategoryIdChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    allowNone: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val sortedCategories = remember(categories) {
        categories.sortedBy { it.displayNameRu().lowercase() }
    }
    val valueText = when {
        isLoading -> "Загрузка…"
        categories.isEmpty() -> "Нет категорий в базе"
        else -> categories.find { it.id == selectedCategoryId }?.displayNameRu()
            ?: if (allowNone) "Не выбрано" else sortedCategories.first().displayNameRu()
    }
    val canOpen = enabled && !isLoading && categories.isNotEmpty()
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val panelMaxHeight = (screenH * 0.58f).coerceAtMost(640.dp).coerceAtLeast(380.dp)
    val panelMinHeight = (screenH * 0.28f).coerceAtMost(340.dp).coerceAtLeast(260.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valueText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Категория") },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.clickable(enabled = canOpen) { expanded = !expanded }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = canOpen) { expanded = !expanded },
            enabled = canOpen
        )
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
        if (expanded && canOpen) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(min = panelMinHeight, max = panelMaxHeight)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (allowNone) {
                        Text(
                            text = "Не выбрано",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectedCategoryIdChange(null)
                                    expanded = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                        HorizontalDivider()
                    }
                    sortedCategories.forEach { cat ->
                        Text(
                            text = cat.displayNameRu(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectedCategoryIdChange(cat.id)
                                    expanded = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VenueFormFields(
    placeName: String,
    onPlaceNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    latitude: String,
    onLatitudeChange: (String) -> Unit,
    longitude: String,
    onLongitudeChange: (String) -> Unit,
    venueCategories: List<VenueCategory>,
    selectedCategoryId: Int?,
    onSelectedCategoryIdChange: (Int?) -> Unit,
    venueCategoriesLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = placeName,
            onValueChange = onPlaceNameChange,
            label = { Text("Название заведения *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Описание") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            enabled = enabled
        )
        OutlinedTextField(
            value = city,
            onValueChange = onCityChange,
            label = { Text("Город") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            enabled = enabled
        )
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Адрес") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            enabled = enabled
        )
        OutlinedTextField(
            value = latitude,
            onValueChange = onLatitudeChange,
            label = { Text("Широта (необяз.)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("55.7558") }
        )
        OutlinedTextField(
            value = longitude,
            onValueChange = onLongitudeChange,
            label = { Text("Долгота (необяз.)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("37.6173") }
        )
        VenueCategoryDropdown(
            categories = venueCategories,
            selectedCategoryId = selectedCategoryId,
            onSelectedCategoryIdChange = onSelectedCategoryIdChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            enabled = enabled,
            isLoading = venueCategoriesLoading
        )
    }
}
