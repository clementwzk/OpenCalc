package com.darkempire78.opencalculator.history

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Data class representing a calculation history entry.
 * Used for storing and displaying past calculations in the history panel.
 * Serialized to JSON for persistent storage in SharedPreferences.
 * 
 * @property calculation The mathematical expression that was calculated
 * @property result The calculated result
 * @property time Timestamp in milliseconds when the calculation was performed
 * @property id Unique identifier for the history entry (auto-generated UUID)
 */
data class History(
    @SerializedName("calculation") var calculation: String,
    @SerializedName("result") var result: String,
    @SerializedName("time") var time: String,
    @SerializedName("id") var id: String = UUID.randomUUID().toString()
)
