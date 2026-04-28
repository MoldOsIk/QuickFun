package com.app.quickfun.data.local

import android.content.Context
import com.app.quickfun.data.remote.SupabaseClient
import com.app.quickfun.domain.model.Place
import io.github.jan.supabase.gotrue.auth

/**
 * Последние известные статусы «моих» заведений на устройстве (по пользователю Supabase).
 * Нужны, чтобы при следующем входе показать snackbar при смене статуса на одобрено/отклонено.
 */
class MyPlacesModerationStatusStore(
    private val appContext: Context
) {

    private fun prefsNameForCurrentUser(): String {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id?.trim().orEmpty()
        return if (uid.isNotEmpty()) "qf_my_place_status_$uid" else "qf_my_place_status_guest"
    }

    suspend fun loadStatusByPlaceId(): Map<String, String> {
        SupabaseClient.client.auth.loadFromStorage()
        val raw = appContext
            .getSharedPreferences(prefsNameForCurrentUser(), Context.MODE_PRIVATE)
            .getString(KEY_LINES, null) ?: return emptyMap()
        return raw.lineSequence()
            .mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0 || tab >= line.length - 1) null
                else line.substring(0, tab) to line.substring(tab + 1).lowercase().trim()
            }
            .toMap()
    }

    suspend fun saveStatusByPlaceId(places: List<Place>) {
        SupabaseClient.client.auth.loadFromStorage()
        if (SupabaseClient.client.auth.currentUserOrNull()?.id.isNullOrBlank()) return
        val raw = places.joinToString("\n") { p ->
            val st = p.status?.lowercase()?.trim().orEmpty()
            "${p.id}\t$st"
        }
        appContext
            .getSharedPreferences(prefsNameForCurrentUser(), Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LINES, raw)
            .apply()
    }

    companion object {
        private const val KEY_LINES = "place_status_lines"
    }
}
