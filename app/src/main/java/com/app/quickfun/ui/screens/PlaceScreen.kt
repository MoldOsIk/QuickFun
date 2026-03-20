package com.app.quickfun.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.app.quickfun.ui.viewmodel.PlaceViewModel

@Composable
fun PlaceScreen(vm: PlaceViewModel) {

    val places by vm.places.collectAsState()

    Box(modifier=Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center){
        LazyColumn {
            items(places) { place ->
                Column {
                    Text(text = place.name)

                    Text(text = place.description ?: "Нет описания")

                    Text(text = "Категория: ${place.categoryName ?: "—"}")

                    Text(text = "Город: ${place.city ?: "—"}")

                    Text(text = "Адрес: ${place.address ?: "—"}")

                    HorizontalDivider()
                }
            }
        }
    }

}