package com.reto01.flood.data

import android.content.Context
import android.content.SharedPreferences
import com.reto01.flood.BuildConfig

class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("flood_settings", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("base_url", BuildConfig.BASE_URL) ?: BuildConfig.BASE_URL
        set(value) = prefs.edit().putString("base_url", value).apply()

    var operator: String
        get() = prefs.getString("operator", "equipo-android") ?: "equipo-android"
        set(value) = prefs.edit().putString("operator", value).apply()

    var mockMode: Boolean
        get() = prefs.getBoolean("mock_mode", false)
        set(value) = prefs.edit().putBoolean("mock_mode", value).apply()

    var lastNotifiedAlertId: Int
        get() = prefs.getInt("last_notified_alert_id", -1)
        set(value) = prefs.edit().putInt("last_notified_alert_id", value).apply()
}
