package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.AuthRepository

class CheckAuthUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(): Boolean {
        return repo.isUserLoggedIn()
    }
}