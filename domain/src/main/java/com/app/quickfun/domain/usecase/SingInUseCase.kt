package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.AuthRepository

class SignInUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) {
        repository.signIn(email, password)
    }
}