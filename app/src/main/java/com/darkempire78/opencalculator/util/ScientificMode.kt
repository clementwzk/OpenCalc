package com.darkempire78.opencalculator.util

import android.content.Context

object ScientificMode {
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

    fun getScientificModeType(type:Int):ScientificModeTypes{
        return when (type) {
            ScientificModeTypes.NOT_ACTIVE.ordinal -> ScientificModeTypes.NOT_ACTIVE
            ScientificModeTypes.ACTIVE.ordinal -> ScientificModeTypes.ACTIVE
            else -> ScientificModeTypes.OFF
        }
    }
}
enum class ScientificModeTypes{
    NOT_ACTIVE, //0
    ACTIVE,//1
    OFF //2
}