package com.app.quickfun.data.remote



import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {

    const val PUBLIC_SUPABASE_URL = "https://gavljlqjkdjbaxmcosui.supabase.co"
    const val VENUE_IMAGES_BUCKET = "venue-images"

    val client = createSupabaseClient(
        supabaseUrl = PUBLIC_SUPABASE_URL,
        supabaseKey = "sb_publishable_1Scc1iT6hbzWSoXQEb9OdA_Cv03j2a_"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}

