package com.darkempire78.opencalculator

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darkempire78.opencalculator.history.History
import com.darkempire78.opencalculator.util.ScientificModeTypes
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private object PreferenceKeys {
    // https://proandroiddev.com/dark-mode-on-android-app-with-kotlin-dc759fc5f0e1
    val THEME = intPreferencesKey("darkempire78.opencalculator.THEME")
    val FORCE_DAY_NIGHT = intPreferencesKey("darkempire78.opencalculator.FORCE_DAY_NIGHT")

    val VIBRATION_STATUS = booleanPreferencesKey("darkempire78.opencalculator.KEY_VIBRATION_STATUS")
    val HISTORY = stringPreferencesKey("darkempire78.opencalculator.HISTORY_ELEMENTS")
    val PREVENT_PHONE_FROM_SLEEPING = booleanPreferencesKey("darkempire78.opencalculator.PREVENT_PHONE_FROM_SLEEPING")
    val HISTORY_SIZE = stringPreferencesKey("darkempire78.opencalculator.HISTORY_SIZE")
    val SCIENTIFIC_MODE_ENABLED_BY_DEFAULT = intPreferencesKey("darkempire78.opencalculator.SCIENTIFIC_MODE_ENABLED_BY_DEFAULT")
    val RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT = booleanPreferencesKey("darkempire78.opencalculator.RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT")
    val NUMBER_PRECISION = stringPreferencesKey("darkempire78.opencalculator.NUMBER_PRECISION")
    val WRITE_NUMBER_INTO_SCIENTIFIC_NOTATION = booleanPreferencesKey("darkempire78.opencalculator.WRITE_NUMBER_INTO_SCIENTIC_NOTATION") // key typo
    val LONG_CLICK_TO_COPY_VALUE = booleanPreferencesKey("darkempire78.opencalculator.LONG_CLICK_TO_COPY_VALUE")
    val ADD_MODULO_BUTTON = booleanPreferencesKey("darkempire78.opencalculator.ADD_MODULO_BUTTON")
    val SPLIT_PARENTHESIS_BUTTON = booleanPreferencesKey("darkempire78.opencalculator.SPLIT_PARENTHESIS_BUTTON")
    val DELETE_HISTORY_ON_SWIPE = booleanPreferencesKey("darkempire78.opencalculator.DELETE_HISTORY_ELEMENT_ON_SWIPE")
    val AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON = booleanPreferencesKey("darkempire78.opencalculator.AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON")
    val MOVE_BACK_BUTTON_LEFT = booleanPreferencesKey("darkempire78.opencalculator.MOVE_BACK_BUTTON_LEFT")
    val NUMBERING_SYSTEM = intPreferencesKey("darkempire78.opencalculator.NUMBERING_SYSTEM")
    val SHOW_ON_LOCK_SCREEN = booleanPreferencesKey("darkempire78.opencalculator.KEY_SHOW_ON_LOCK_SCREEN")
}

private val Context.dataStore by preferencesDataStore(
    name = "datastore",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, context.packageName + "_preferences") { sharedPrefs, currentData ->
                val mutablePreferences = currentData.toMutablePreferences()
                // Migrate the scientific mode
                // The migrated ordinal value according to these rules:
                //        - true → ScientificModeTypes.ACTIVE.ordinal (1)
                //        - false → ScientificModeTypes.NOT_ACTIVE.ordinal (0)
                // - Invalid/unknown types → ScientificModeTypes.OFF.ordinal (2)
                val oldScientificMode = sharedPrefs.getAll()[PreferenceKeys.SCIENTIFIC_MODE_ENABLED_BY_DEFAULT.name]
                val migratedValue: Int = when (oldScientificMode) {
                    is Boolean -> {
                        if (oldScientificMode) {
                            ScientificModeTypes.ACTIVE.ordinal
                        } else {
                            ScientificModeTypes.NOT_ACTIVE.ordinal
                        }
                    }
                    is Int -> {
                        if (oldScientificMode in ScientificModeTypes.entries.toTypedArray().indices){
                            oldScientificMode
                        } else {
                            ScientificModeTypes.OFF.ordinal
                        }
                    }
                    else -> ScientificModeTypes.OFF.ordinal
                }
                // set the migrated value
                mutablePreferences[PreferenceKeys.SCIENTIFIC_MODE_ENABLED_BY_DEFAULT] = migratedValue

                mutablePreferences
            }
        )
    }
)

class MyPreferences(private val context: Context) {

    private fun <T> readSync(key: Preferences.Key<T>, default: T): T = runBlocking {
        context.dataStore.data.first()[key] ?: default
    }

    private fun <T> writeSync(key: Preferences.Key<T>, value: T) = runBlocking {
        context.dataStore.edit { prefs -> prefs[key] = value }
        return@runBlocking
    }

    var theme: Int
        get() = readSync(PreferenceKeys.THEME, -1)
        set(value) = writeSync(PreferenceKeys.THEME, value)

    var forceDayNight: Int
        get() = readSync(PreferenceKeys.FORCE_DAY_NIGHT, MODE_NIGHT_UNSPECIFIED)
        set(value) = writeSync(PreferenceKeys.FORCE_DAY_NIGHT, value)

    var vibrationMode: Boolean
        get() = readSync(PreferenceKeys.VIBRATION_STATUS, true)
        set(value) = writeSync(PreferenceKeys.VIBRATION_STATUS, value)

    var scientificMode:Int
        get() = readSync(PreferenceKeys.SCIENTIFIC_MODE_ENABLED_BY_DEFAULT, ScientificModeTypes.OFF.ordinal)
        set(value) = writeSync(PreferenceKeys.SCIENTIFIC_MODE_ENABLED_BY_DEFAULT, value)

    var useRadiansByDefault: Boolean
        get() = readSync(PreferenceKeys.RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT, false)
        set(value) = writeSync(PreferenceKeys.RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT, value)

    private var historyJson: String
        get() = readSync(PreferenceKeys.HISTORY,  "")
        set(value) = writeSync(PreferenceKeys.HISTORY, value)

    var preventPhoneFromSleepingMode: Boolean
        get() = readSync(PreferenceKeys.PREVENT_PHONE_FROM_SLEEPING, false)
        set(value) = writeSync(PreferenceKeys.PREVENT_PHONE_FROM_SLEEPING, value)
    var historySize: String
        get() = readSync(PreferenceKeys.HISTORY_SIZE, "50")
        set(value) = writeSync(PreferenceKeys.HISTORY_SIZE, value)
    var numberPrecision: String
        get() = readSync(PreferenceKeys.NUMBER_PRECISION, "10")
        set(value) = writeSync(PreferenceKeys.NUMBER_PRECISION, value)
    var numberIntoScientificNotation: Boolean
        get() = readSync(PreferenceKeys.WRITE_NUMBER_INTO_SCIENTIFIC_NOTATION, false)
        set(value) = writeSync(PreferenceKeys.WRITE_NUMBER_INTO_SCIENTIFIC_NOTATION, value)
    var longClickToCopyValue: Boolean
        get() = readSync(PreferenceKeys.LONG_CLICK_TO_COPY_VALUE, true)
        set(value) = writeSync(PreferenceKeys.LONG_CLICK_TO_COPY_VALUE, value)
    var addModuloButton: Boolean
        get() = readSync(PreferenceKeys.ADD_MODULO_BUTTON, true)
        set(value) = writeSync(PreferenceKeys.ADD_MODULO_BUTTON, value)
    var splitParenthesisButton: Boolean
        get() = readSync(PreferenceKeys.SPLIT_PARENTHESIS_BUTTON, false)
        set(value) = writeSync(PreferenceKeys.SPLIT_PARENTHESIS_BUTTON, value)
    var deleteHistoryOnSwipe: Boolean
        get() = readSync(PreferenceKeys.DELETE_HISTORY_ON_SWIPE, false)
        set(value) = writeSync(PreferenceKeys.DELETE_HISTORY_ON_SWIPE, value)

    var autoSaveCalculationWithoutEqualButton: Boolean
        get() = readSync(PreferenceKeys.AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON, true)
        set(value) = writeSync(PreferenceKeys.AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON, value)

    var moveBackButtonLeft: Boolean
        get() = readSync(PreferenceKeys.MOVE_BACK_BUTTON_LEFT, false)
        set(value) = writeSync(PreferenceKeys.MOVE_BACK_BUTTON_LEFT, value)

    var numberingSystem: Int
        get() = readSync(PreferenceKeys.NUMBERING_SYSTEM, 0)
        set(value) = writeSync(PreferenceKeys.NUMBERING_SYSTEM, value)

    var showOnLockScreen: Boolean
        get() = readSync(PreferenceKeys.SHOW_ON_LOCK_SCREEN, true)
        set(value) = writeSync(PreferenceKeys.SHOW_ON_LOCK_SCREEN, value)

    fun getHistory(): MutableList<History> {
        val gson = Gson()
        return if (historyJson.isNotEmpty()) {
            try {
                gson.fromJson(historyJson, Array<History>::class.java).asList().toMutableList()
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
        while (historySize.toInt() > 0 && history2.size > historySize.toInt()) {
            history2.removeAt(0)
        }
        historyJson = gson.toJson(history2) // Convert to json
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
