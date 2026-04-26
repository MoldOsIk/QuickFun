package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.AuthRepository

class SignOutUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.signOut()
}
