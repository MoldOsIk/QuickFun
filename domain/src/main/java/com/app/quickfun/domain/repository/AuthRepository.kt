package com.app.quickfun.domain.repository

import com.app.quickfun.domain.model.SignUpResult

interface AuthRepository {
    suspend fun signUp(email: String, password: String, displayName: String? = null): SignUpResult

    suspend fun signIn(email: String, password: String)

    suspend fun signOut()

    suspend fun isUserLoggedIn(): Boolean

}