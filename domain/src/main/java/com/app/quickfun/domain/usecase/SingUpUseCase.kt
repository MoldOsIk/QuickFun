package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.model.SignUpResult
import com.app.quickfun.domain.repository.AuthRepository

class SignUpUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String? = null,
        phoneE164: String? = null
    ): SignUpResult =
        repository.signUp(email, password, displayName, phoneE164)
}