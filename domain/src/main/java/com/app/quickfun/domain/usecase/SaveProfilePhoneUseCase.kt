package com.app.quickfun.domain.usecase

import com.app.quickfun.domain.repository.ProfileRepository

class SaveProfilePhoneUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(phoneE164: String?) = repository.savePhoneE164(phoneE164)
}
