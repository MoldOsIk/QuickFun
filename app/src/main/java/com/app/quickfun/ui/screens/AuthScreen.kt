package com.app.quickfun.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.app.quickfun.ui.viewmodel.AuthState
import com.app.quickfun.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(vm: AuthViewModel) {

    val state by vm.state.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    when (state) {

        is AuthState.Loading -> {
            CircularProgressIndicator()
        }

        is AuthState.Unauthorized -> {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { vm.signIn(email, password) }) {
                    Text("Войти")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { vm.signUp(email, password) }) {
                    Text("Зарегистрироваться")
                }
            }
        }

        is AuthState.AwaitingEmailConfirmation -> {
            val emailSentTo = (state as AuthState.AwaitingEmailConfirmation).email
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Аккаунт создан. Откройте письмо и подтвердите email, затем войдите с теми же данными."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = emailSentTo)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { vm.dismissEmailConfirmation() }) {
                    Text("К экрану входа")
                }
            }
        }

        is AuthState.Error -> {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text((state as AuthState.Error).message)
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { vm.toUnauthorizedAfterError() }) {
                    Text("Назад")
                }
            }
        }

        is AuthState.Authorized -> {
            // MainActivity переключит на PlaceScreen
        }
    }
}