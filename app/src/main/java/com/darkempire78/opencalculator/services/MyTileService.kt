package com.darkempire78.opencalculator.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.darkempire78.opencalculator.activities.MainActivity

/**
 * Quick Settings Tile service for launching the calculator from the notification panel.
 * Provides quick access to the calculator without opening the app drawer.
 * Requires Android N (API 24) or higher for Quick Settings Tile support.
 */
@RequiresApi(Build.VERSION_CODES.N)
class MyTileService : TileService() {

    /**
     * Called when the user taps on the Quick Settings tile.
     * Launches MainActivity and collapses the Quick Settings panel.
     */
    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()

        // Create intent to launch the main calculator activity
        // Intent to launch MainActivity
        val intent = Intent(this, MainActivity::class.java)
        
        // Create a PendingIntent for secure activity launching
        // Create a PendingIntent from the intent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Use appropriate method based on Android version
        // Check the SDK version to determine which method to use
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: Use modern PendingIntent-based API
            startActivityAndCollapse(pendingIntent)
        } else {
            // Android 13 and below: Use legacy Intent-based approach
            // For older versions, convert PendingIntent to Intent and start the activity
            val newIntent = Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(newIntent)
        }
    }
}
