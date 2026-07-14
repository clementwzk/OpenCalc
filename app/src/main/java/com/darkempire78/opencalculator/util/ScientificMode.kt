package com.darkempire78.opencalculator.util

import android.content.Context

/**
 * Utility object for managing scientific mode configurations.
 * Provides helpers for converting between enum types and localized descriptions.
 */
object ScientificMode {
    /**
     * Gets the localized description string for a scientific mode type.
     * @param context The context for accessing string resources
     * @param scientificModeTypes The scientific mode type to get description for
     * @return Localized description string
     */
    fun getScientificModeTypeDescription(
        context: Context,
        scientificModeTypes: ScientificModeTypes
    ): String {
        return when (scientificModeTypes) {
            ScientificModeTypes.OFF -> context.getString(com.darkempire78.opencalculator.R.string.settings_general_scientific_mode_hide_desc)
            ScientificModeTypes.NOT_ACTIVE -> context.getString(com.darkempire78.opencalculator.R.string.settings_general_scientific_mode_deactivate_desc)
            ScientificModeTypes.ACTIVE -> context.getString(com.darkempire78.opencalculator.R.string.settings_general_scientific_mode_desc)
        }
    }

    /**
     * Converts an integer ordinal value to its corresponding ScientificModeTypes enum.
     * @param type The ordinal value (0, 1, or 2)
     * @return The corresponding enum value, defaults to OFF for invalid values
     */
    fun getScientificModeType(type:Int):ScientificModeTypes{
        return when (type) {
            ScientificModeTypes.NOT_ACTIVE.ordinal -> ScientificModeTypes.NOT_ACTIVE
            ScientificModeTypes.ACTIVE.ordinal -> ScientificModeTypes.ACTIVE
            else -> ScientificModeTypes.OFF
        }
    }
}

/**
 * Enum representing the three states of scientific mode in the calculator.
 * - NOT_ACTIVE (0): Scientific mode buttons visible but collapsed by default
 * - ACTIVE (1): Scientific mode buttons visible and expanded by default
 * - OFF (2): Scientific mode buttons completely hidden
 */
enum class ScientificModeTypes{
    NOT_ACTIVE, //0
    ACTIVE,//1
    OFF //2
}
