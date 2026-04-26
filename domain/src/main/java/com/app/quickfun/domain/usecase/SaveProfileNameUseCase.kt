package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.ProfileRepository

class SaveProfileNameUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(name: String) = repository.saveName(name)
}
