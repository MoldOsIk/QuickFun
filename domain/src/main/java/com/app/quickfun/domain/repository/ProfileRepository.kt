package com.app.quickfun.domain.repository

import com.app.quickfun.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun saveName(name: String)
}
