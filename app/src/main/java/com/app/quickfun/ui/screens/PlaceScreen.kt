package com.app.quickfun.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import com.app.quickfun.ui.viewmodel.PlaceViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PlaceScreen(vm: PlaceViewModel) {

    val places by vm.places.collectAsState()

    LazyColumn {
        items(places) { place ->
            Column {
                Text(text = place.name)

                Text(text = place.description ?: "Нет описания")

                Text(text = "Категория: ${place.categoryName ?: "—"}")

                Text(text = "Город: ${place.city ?: "—"}")

                Text(text = "Адрес: ${place.address ?: "—"}")

                Divider()
            }
        }
    }
}