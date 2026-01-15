package com.darkempire78.opencalculator

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate.*

/**
 * Application class for OpenCalc.
 * Handles app-wide initialization including theme configuration.
 */
class OpenCalcApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply forced day/night mode on app startup if user has overridden system default
        // if the theme is overriding the system, the first creation doesn't work properly
        val forceDayNight = MyPreferences(this).forceDayNight
        if (forceDayNight != MODE_NIGHT_UNSPECIFIED && forceDayNight != MODE_NIGHT_FOLLOW_SYSTEM)
            setDefaultNightMode(forceDayNight)
    }
}