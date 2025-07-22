package com.darkempire78.opencalculator

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import com.darkempire78.opencalculator.history.History
import com.google.gson.Gson

class MyPreferences(context: Context) {

    var ctx = context

    // https://proandroiddev.com/dark-mode-on-android-app-with-kotlin-dc759fc5f0e1
    companion object {
        private const val THEME = "darkempire78.opencalculator.THEME"
        private const val FORCE_DAY_NIGHT = "darkempire78.opencalculator.FORCE_DAY_NIGHT"

        private const val KEY_VIBRATION_STATUS = "darkempire78.opencalculator.KEY_VIBRATION_STATUS"
        private const val KEY_HISTORY = "darkempire78.opencalculator.HISTORY_ELEMENTS"
        private const val KEY_PREVENT_PHONE_FROM_SLEEPING = "darkempire78.opencalculator.PREVENT_PHONE_FROM_SLEEPING"
        private const val KEY_HISTORY_SIZE = "darkempire78.opencalculator.HISTORY_SIZE"
        private const val KEY_SCIENTIFIC_MODE_ENABLED_BY_DEFAULT = "darkempire78.opencalculator.SCIENTIFIC_MODE_ENABLED_BY_DEFAULT"
        private const val KEY_RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT = "darkempire78.opencalculator.RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT"
        private const val KEY_NUMBER_PRECISION = "darkempire78.opencalculator.NUMBER_PRECISION"
        private const val KEY_WRITE_NUMBER_INTO_SCIENTIC_NOTATION = "darkempire78.opencalculator.WRITE_NUMBER_INTO_SCIENTIC_NOTATION"
        private const val KEY_LONG_CLICK_TO_COPY_VALUE = "darkempire78.opencalculator.LONG_CLICK_TO_COPY_VALUE"
        private const val KEY_ADD_MODULO_BUTTON = "darkempire78.opencalculator.ADD_MODULO_BUTTON"
        private const val KEY_SPLIT_PARENTHESIS_BUTTON = "darkempire78.opencalculator.SPLIT_PARENTHESIS_BUTTON"
        private const val KEY_DELETE_HISTORY_ON_SWIPE = "darkempire78.opencalculator.DELETE_HISTORY_ELEMENT_ON_SWIPE"
        private const val KEY_AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON = "darkempire78.opencalculator.AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON"
        private const val KEY_MOVE_BACK_BUTTON_LEFT = "darkempire78.opencalculator.MOVE_BACK_BUTTON_LEFT"
        private const val KEY_NUMBERING_SYSTEM = "darkempire78.opencalculator.NUMBERING_SYSTEM"
        private const val KEY_SHOW_ON_LOCK_SCREEN = "darkempire78.opencalculator.KEY_SHOW_ON_LOCK_SCREEN"
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var theme = preferences.getInt(THEME, -1)
        set(value) = preferences.edit().putInt(THEME, value).apply()
    var forceDayNight = preferences.getInt(FORCE_DAY_NIGHT, MODE_NIGHT_UNSPECIFIED)
        set(value) = preferences.edit().putInt(FORCE_DAY_NIGHT, value).apply()

    var vibrationMode = preferences.getBoolean(KEY_VIBRATION_STATUS, true)
        set(value) = preferences.edit().putBoolean(KEY_VIBRATION_STATUS, value).apply()
    private val currentScientificModeTypes= MyPreferenceMigrator.migrateScientificMode(preferences, KEY_SCIENTIFIC_MODE_ENABLED_BY_DEFAULT)
    var scientificMode = preferences.getInt(KEY_SCIENTIFIC_MODE_ENABLED_BY_DEFAULT, currentScientificModeTypes)
        set(value) = preferences.edit().putInt(KEY_SCIENTIFIC_MODE_ENABLED_BY_DEFAULT, value).apply()
    var useRadiansByDefault = preferences.getBoolean(KEY_RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT, false)
        set(value) = preferences.edit().putBoolean(KEY_RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT, value).apply()
    private var history = preferences.getString(KEY_HISTORY, null)
        set(value) = preferences.edit().putString(KEY_HISTORY, value).apply()
    var preventPhoneFromSleepingMode = preferences.getBoolean(KEY_PREVENT_PHONE_FROM_SLEEPING, false)
        set(value) = preferences.edit().putBoolean(KEY_PREVENT_PHONE_FROM_SLEEPING, value).apply()
    var historySize = preferences.getString(KEY_HISTORY_SIZE, "50")
        set(value) = preferences.edit().putString(KEY_HISTORY_SIZE, value).apply()
    var numberPrecision = preferences.getString(KEY_NUMBER_PRECISION, "10")
        set(value) = preferences.edit().putString(KEY_NUMBER_PRECISION, value).apply()
    var numberIntoScientificNotation = preferences.getBoolean(KEY_WRITE_NUMBER_INTO_SCIENTIC_NOTATION, false)
        set(value) = preferences.edit().putBoolean(KEY_WRITE_NUMBER_INTO_SCIENTIC_NOTATION, value).apply()
    var longClickToCopyValue = preferences.getBoolean(KEY_LONG_CLICK_TO_COPY_VALUE, true)
        set(value) = preferences.edit().putBoolean(KEY_LONG_CLICK_TO_COPY_VALUE, value).apply()
    var addModuloButton = preferences.getBoolean(KEY_ADD_MODULO_BUTTON, true)
        set(value) = preferences.edit().putBoolean(KEY_ADD_MODULO_BUTTON, value).apply()
    var splitParenthesisButton = preferences.getBoolean(KEY_SPLIT_PARENTHESIS_BUTTON, false)
        set(value) = preferences.edit().putBoolean(KEY_SPLIT_PARENTHESIS_BUTTON, value).apply()
    var deleteHistoryOnSwipe = preferences.getBoolean(KEY_DELETE_HISTORY_ON_SWIPE, true)
        set(value) = preferences.edit().putBoolean(KEY_DELETE_HISTORY_ON_SWIPE, value).apply()

    var autoSaveCalculationWithoutEqualButton = preferences.getBoolean(KEY_AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON, true)
        set(value) = preferences.edit().putBoolean(KEY_AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON, value).apply()

    var moveBackButtonLeft = preferences.getBoolean(KEY_MOVE_BACK_BUTTON_LEFT, false)
        set(value) = preferences.edit().putBoolean(KEY_MOVE_BACK_BUTTON_LEFT, value).apply()

    var numberingSystem = preferences.getInt(KEY_NUMBERING_SYSTEM, 0)
        set(value) = preferences.edit().putInt(KEY_NUMBERING_SYSTEM, value).apply()

    var showOnLockScreen = preferences.getBoolean(KEY_SHOW_ON_LOCK_SCREEN, true)
        set(value) = preferences.edit().putBoolean(KEY_SHOW_ON_LOCK_SCREEN, value).apply()


    fun getHistory(): MutableList<History> {
        val gson = Gson()

        val historyJson = preferences.getString(KEY_HISTORY, null)

        return if (historyJson != null) {
            try {
                val list = gson.fromJson(historyJson, Array<History>::class.java).asList().toMutableList()
                list
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }



    fun saveHistory(history: List<History>){
        val gson = Gson()
        val history2 = history.toMutableList()
        while (historySize!!.toInt() > 0 && history2.size > historySize!!.toInt()) {
            history2.removeAt(0)
        }
        MyPreferences(ctx).history = gson.toJson(history2) // Convert to json
    }

    fun getHistoryElementById(id: String): History? {
        val history = getHistory()
        return history.find { it.id == id }
    }

    fun updateHistoryElementById(id: String, history: History) {
        val historyList = getHistory()
        val index = historyList.indexOfFirst { it.id == id }
        if (index != -1) {
            historyList[index] = history
            saveHistory(historyList)
        }
    }
}
