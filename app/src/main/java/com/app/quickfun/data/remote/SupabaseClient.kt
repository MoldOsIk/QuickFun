package com.app.quickfun.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth

object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = "https://gavljlqjkdjbaxmcosui.supabase.co",
        supabaseKey = "sb_publishable_1Scc1iT6hbzWSoXQEb9OdA_Cv03j2a_"
    ) {
        install(Auth)
        install(Postgrest)
    }
}