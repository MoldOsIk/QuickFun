package com.app.quickfun.domain.repository

import com.app.quickfun.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun saveName(name: String)
    suspend fun savePhoneE164(phoneE164: String?)
}
