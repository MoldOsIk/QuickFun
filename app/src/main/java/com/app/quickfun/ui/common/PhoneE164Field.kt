package com.app.quickfun.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.quickfun.domain.phone.PhoneE164

/**
 * Код страны (выпадающий список) + поле только цифр, длина [DialCountry.nationalDigits].
 * [onE164Change] — полный E.164 или null, если номер неполный / пустой.
 */
@Composable
fun PhoneE164Field(
    selectedCountry: DialCountry,
    onCountryChange: (DialCountry) -> Unit,
    nationalDigits: String,
    onNationalDigitsChange: (String) -> Unit,
    onE164Change: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit = { Text("Телефон (необязательно)") }
) {
    val filteredNational = PhoneE164.digitsOnly(nationalDigits).take(selectedCountry.nationalDigits)
    val onE164Updated = rememberUpdatedState(onE164Change)
    LaunchedEffect(selectedCountry.iso, filteredNational) {
        val built = PhoneE164.validE164IfComplete(
            selectedCountry.dialDigits,
            filteredNational,
            selectedCountry.nationalDigits
        )
        onE164Updated.value(built)
    }

    var expanded by remember { mutableStateOf(false) }

    Column(modifier) {
        Box(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = "${selectedCountry.nameRu} +${selectedCountry.dialDigits}",
                onValueChange = {},
                readOnly = true,
                label = { Text("Страна / код") },
                trailingIcon = {
                    IconButton(
                        onClick = { expanded = true },
                        enabled = enabled
                    ) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбор страны")
                    }
                },
                modifier = Modifier
                    .then(
                        if (enabled) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { expanded = true }
                        } else {
                            Modifier
                        }
                    )
                    .fillMaxWidth(),
                enabled = enabled
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                quickFunDialCountries.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.nameRu}  +${c.dialDigits}  (${c.nationalDigits} цифр)") },
                        onClick = {
                            onCountryChange(c)
                            onNationalDigitsChange("")
                            expanded = false
                        }
                    )
                }
            }
        }
        val remaining = (selectedCountry.nationalDigits - filteredNational.length).coerceAtLeast(0)
        OutlinedTextField(
            value = filteredNational,
            onValueChange = { raw ->
                onNationalDigitsChange(PhoneE164.digitsOnly(raw).take(selectedCountry.nationalDigits))
            },
            label = label,
            placeholder = {
                Text(
                    if (remaining > 0) "Ещё $remaining цифр" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = enabled,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}
