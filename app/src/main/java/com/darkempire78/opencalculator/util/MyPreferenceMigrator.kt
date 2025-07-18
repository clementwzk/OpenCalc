import android.content.SharedPreferences
import com.darkempire78.opencalculator.util.ScientificModeTypes

/**
 * Handles migration of SharedPreferences values between different data types.
 * Primarily used for converting boolean flags to enum ordinal representations.
 */
object MyPreferenceMigrator {


    /**
     * Migrates a boolean preference value to its corresponding enum ordinal representation.
     *
     * @param sharedPreferences The SharedPreferences instance containing the value to migrate
     * @param key The preference key to migrate
     * @return The migrated ordinal value according to these rules:
     *         - true → ScientificModeTypes.ACTIVE.ordinal (1)
     *         - false → ScientificModeTypes.NOT_ACTIVE.ordinal (0)
     *         - Invalid/unknown types → ScientificModeTypes.OFF.ordinal (2)
     * @throws IllegalArgumentException if sharedPreferences is null
     */
    fun migrateScientificMode(sharedPreferences: SharedPreferences, key: String): Int {

        return when (val value = sharedPreferences.all[key]) {
            // Boolean case - convert to corresponding ordinal
            is Boolean -> {
                val modeOrdinal = when {
                    value -> ScientificModeTypes.ACTIVE.ordinal
                    else -> ScientificModeTypes.NOT_ACTIVE.ordinal
                }
                saveMigratedValue(sharedPreferences, key, modeOrdinal)
            }

            // Already migrated case (stored as Int)
            is Int -> {
                if (value in ScientificModeTypes.entries.toTypedArray().indices) {
                    value // Return existing valid ordinal
                } else {
                    resetToDefault(sharedPreferences, key)
                }
            }
            // All other cases reset to OFF
            else -> resetToDefault(sharedPreferences, key)
        }
    }

    /**
     * Safely saves a migrated value to SharedPreferences.
     *
     * @param sharedPreferences The SharedPreferences instance to modify
     * @param key The preference key to update
     * @param value The ordinal value to save
     * @return The saved ordinal value
     * @throws IllegalArgumentException if value is not a valid ordinal
     */
    private fun saveMigratedValue(
        sharedPreferences: SharedPreferences,
        key: String,
        value: Int
    ): Int {
        require(value in ScientificModeTypes.entries.toTypedArray().indices) {
            "Invalid ordinal value $value for ScientificModeTypes"
        }

        sharedPreferences.edit()
            .remove(key) // Remove old value first
            .putInt(key, value)
            .apply()
        return value
    }

    /**
     * Resets a preference to the default OFF state.
     *
     * @param sharedPreferences The SharedPreferences instance to modify
     * @param key The preference key to reset
     * @return The default ordinal value (ScientificModeTypes.OFF.ordinal)
     */
    private fun resetToDefault(sharedPreferences: SharedPreferences, key: String): Int {
        return saveMigratedValue(
            sharedPreferences,
            key,
            ScientificModeTypes.OFF.ordinal
        )
    }

    /**
     * Helper function to safely retrieve the current scientific mode.
     *
     * @param sharedPreferences The SharedPreferences instance to read from
     * @param key The preference key to read
     * @return The current ScientificModeTypes enum value
     */
    fun getCurrentMode(sharedPreferences: SharedPreferences, key: String): ScientificModeTypes {
        return try {
            val ordinal = sharedPreferences.getInt(key, ScientificModeTypes.OFF.ordinal)
            ScientificModeTypes.entries.getOrNull(ordinal) ?: ScientificModeTypes.OFF
        } catch (e: Exception) {
            ScientificModeTypes.OFF
        }
    }
}