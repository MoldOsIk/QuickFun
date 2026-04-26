package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.ProfileRepository

class GetProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke() = repository.getProfile()
}
