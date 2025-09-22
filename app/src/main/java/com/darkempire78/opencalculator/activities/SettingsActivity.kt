package com.darkempire78.opencalculator.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.darkempire78.opencalculator.MyPreferences
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.Themes
import com.darkempire78.opencalculator.calculator.parser.NumberingSystem
import com.darkempire78.opencalculator.util.ScientificMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Themes
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        // Fix view for SDK 35
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )

            // Return the insets to allow other listeners to consume them
            insets
        }

        setContentView(R.layout.settings_activity)

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment())
                    .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Change the status bar color
        if (MyPreferences(this).theme == 1) { // Amoled theme
            window.statusBarColor = ContextCompat.getColor(this, R.color.amoled_background_color)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        }

        // back button
        findViewById<ImageView>(R.id.settings_back_button).setOnClickListener {
            finish()
        }

    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val appLanguagePreference = findPreference<Preference>("darkempire78.opencalculator.APP_LANGUAGE")

            // remove the app language button if you are using an Android version lower than v33 (Android 13)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                appLanguagePreference?.isVisible = false
            } else {
                // Display the current selected language
                appLanguagePreference?.summary = Locale.getDefault().displayLanguage
            }

            // Select app language button
            appLanguagePreference?.setOnPreferenceClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launchChangeAppLanguageIntent()
                }
                true
            }

            // Theme button
            val appThemePreference = findPreference<Preference>("darkempire78.opencalculator.APP_THEME_SELECTOR")

            appThemePreference?.summary = Themes(this.requireContext()).getThemeNameFromId(
                MyPreferences(this.requireContext()).theme)

            appThemePreference?.setOnPreferenceClickListener {
                Themes.openDialogThemeSelector(this.requireContext())
                true
            }


            // Numbering System button
            val appNumberingSystemPreference =
                findPreference<Preference>("darkempire78.opencalculator.NUMBERING_SYSTEM")

            appNumberingSystemPreference?.summary =
                NumberingSystem.getDescription(MyPreferences(this.requireContext()).numberingSystem)

            appNumberingSystemPreference?.setOnPreferenceClickListener {
                openDialogNumberingSystemSelector(this.requireContext())
                true
            }


            // Numbering System button  settings_select_scientificMode_system
            val scientificModeTypesSystemPreference =
                findPreference<Preference>("darkempire78.opencalculator.SCIENTIFIC_MODE_ENABLED_BY_DEFAULT")

            val storedType = MyPreferences(requireContext()).scientificMode
           val scientificModeType = ScientificMode.getScientificModeType(storedType)
            val typeDescription=ScientificMode.getScientificModeTypeDescription(requireContext(),scientificModeType)
            scientificModeTypesSystemPreference?.summary = typeDescription
            scientificModeTypesSystemPreference?.setOnPreferenceClickListener {
                openDialogScientificModeSelector(this.requireContext())
                true
            }

        }

        private fun openDialogNumberingSystemSelector(context: Context) {

            val preferences = MyPreferences(context)

            val builder = MaterialAlertDialogBuilder(context)
            builder.background = ContextCompat.getDrawable(context, R.drawable.rounded)

            val numberingSystem = hashMapOf(
                0 to NumberingSystem.INTERNATIONAL.description,
                1 to NumberingSystem.INDIAN.description
            )

            val checkedItem = preferences.numberingSystem

            builder.setSingleChoiceItems(
                numberingSystem.values.toTypedArray(),
                checkedItem
            ) { dialog, which ->
                when (which) {
                    0 -> {
                        preferences.numberingSystem = 0
                    }

                    1 -> {
                        preferences.numberingSystem = 1
                    }
                }
                dialog.dismiss()
                reloadActivity(requireContext())
            }
            val dialog = builder.create()
            dialog.show()
        }

        private fun reloadActivity(context: Context) {
            (context as Activity).finish()
            ContextCompat.startActivity(context, context.intent, null)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun launchChangeAppLanguageIntent() {
            try {
                Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                    startActivity(this)
                }
            } catch (e: Exception) {
                try {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                        startActivity(this)
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
        private fun openDialogScientificModeSelector(context: Context) {

            val preferences = MyPreferences(context)

            val builder = MaterialAlertDialogBuilder(context)
            builder.background = ContextCompat.getDrawable(context, R.drawable.rounded)

            val scientificMode = hashMapOf(
                0 to context.getString(R.string.settings_general_scientific_mode_deactivate_desc),
                1 to context.getString(R.string.settings_general_scientific_mode_desc),
                2 to context.getString(R.string.settings_general_scientific_mode_hide_desc)
            )

            val checkedItem = preferences.scientificMode

            builder.setSingleChoiceItems(
                scientificMode.values.toTypedArray(),
                checkedItem
            ) { dialog, which ->
                when (which) {
                    0 -> {
                        preferences.scientificMode = 0
                    }

                    1 -> {
                        preferences.scientificMode = 1
                    }

                    2-> {
                        preferences.scientificMode = 2
                    }

                }
                dialog.dismiss()
                reloadActivity(requireContext())
            }
            val dialog = builder.create()
            dialog.show()
        }
    }
}
