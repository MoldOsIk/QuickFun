package com.app.quickfun

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class QuickFunApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val key = BuildConfig.MAPKIT_API_KEY.trim()
        if (key.isNotEmpty()) {
            MapKitFactory.setApiKey(key)
        }
        MapKitFactory.initialize(this)
    }
}
