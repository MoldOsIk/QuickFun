package com.app.quickfun.data.repository

import com.app.quickfun.data.remote.SupabaseClient
import com.app.quickfun.domain.model.SignUpResult
import com.app.quickfun.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

class AuthRepositoryImpl : AuthRepository {

    override suspend fun signUp(email: String, password: String): SignUpResult {
        SupabaseClient.client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val sessionActive = SupabaseClient.client.auth.currentSessionOrNull() != null
        return SignUpResult(sessionActive = sessionActive, email = email)
    }

    override suspend fun signIn(email: String, password: String) {
        SupabaseClient.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        SupabaseClient.client.auth.signOut()
    }
    override suspend fun isUserLoggedIn(): Boolean {
        return SupabaseClient.client.auth.currentSessionOrNull() != null
    }
}