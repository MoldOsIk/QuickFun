package com.app.quickfun.data.repository

import android.util.Log
import com.app.quickfun.data.remote.SupabaseClient
import com.app.quickfun.data.remote.dto.UserRoleRowDto
import com.app.quickfun.data.remote.dto.UserNameUpdateDto
import com.app.quickfun.data.remote.dto.UserPhoneE164UpdateDto
import com.app.quickfun.data.remote.dto.UserProfileRowDto
import com.app.quickfun.domain.model.UserProfile
import com.app.quickfun.domain.repository.ProfileRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class ProfileRepositoryImpl : ProfileRepository {
    companion object {
        private const val TAG = "ProfileRepository"
    }

    override suspend fun getProfile(): UserProfile {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentSessionOrNull() == null) {
            throw IllegalStateException("No active session")
        }
        val userInfo = SupabaseClient.client.auth.retrieveUserForCurrentSession(updateSession = true)

        val userRow = try {
            val rows = SupabaseClient.client
                .from("users")
                .select(
                    Columns.raw(
                        """
                        id,
                        name,
                        phone_e164
                        """
                    )
                ) {
                    filter {
                        eq("id", userInfo.id)
                    }
                }
                .decodeList<UserProfileRowDto>()
            rows.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading users/user_roles for ${userInfo.id}", e)
            null
        }

        val roles = try {
            SupabaseClient.client
                .from("user_roles")
                .select(
                    Columns.raw(
                        """
                        roles(name)
                        """
                    )
                ) {
                    filter {
                        eq("user_id", userInfo.id)
                    }
                }
                .decodeList<UserRoleRowDto>()
                .mapNotNull { it.roles?.name }
                .distinct()
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading roles for ${userInfo.id}", e)
            emptyList()
        }

        return UserProfile(
            id = userInfo.id,
            email = userInfo.email.orEmpty(),
            name = userRow?.name.orEmpty(),
            phoneE164 = userRow?.phone_e164?.trim()?.takeIf { it.isNotEmpty() },
            roles = roles
        )
    }

    override suspend fun saveName(name: String) {
        SupabaseClient.client.auth.loadFromStorage()
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            ?: throw IllegalStateException("No active session")

        try {
            SupabaseClient.client.from("users").update(
                UserNameUpdateDto(name = name.ifBlank { null })
            ) {
                filter { eq("id", currentUser.id) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving user name for ${currentUser.id}", e)
            throw e
        }
    }

    override suspend fun savePhoneE164(phoneE164: String?) {
        SupabaseClient.client.auth.loadFromStorage()
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            ?: throw IllegalStateException("No active session")
        val normalized = phoneE164?.trim()?.takeIf { it.isNotEmpty() }
        try {
            SupabaseClient.client.from("users").update(
                UserPhoneE164UpdateDto(phone_e164 = normalized)
            ) {
                filter { eq("id", currentUser.id) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving phone for ${currentUser.id}", e)
            throw e
        }
    }
}
