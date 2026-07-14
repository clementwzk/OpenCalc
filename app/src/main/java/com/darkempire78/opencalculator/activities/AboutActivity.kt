package com.darkempire78.opencalculator.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.darkempire78.opencalculator.BuildConfig
import com.darkempire78.opencalculator.MyPreferences
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.Themes
import com.darkempire78.opencalculator.databinding.ActivityAboutBinding
import com.darkempire78.opencalculator.dialogs.DonationDialog

/**
 * Activity displaying information about the app, including:
 * - App version and build number
 * - Links to external resources (translation, rating, GitHub, Discord)
 * - Privacy policy and license information
 * - Easter egg on version tap
 */
class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply theme settings
        // Themes
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        // Change the status bar color
        if (MyPreferences(this).theme == 1) { // Amoled theme
            window.statusBarColor = ContextCompat.getColor(this, R.color.amoled_background_color)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        }

        // Initialize view binding
        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Set app version
        val versionName =  this.getString(R.string.about_other_version) + " "+ BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
        binding.aboutAppVersion.text = versionName

        // Back button handlers
        // back button
        binding.aboutBackButton.setOnClickListener {
            finish()
        }
        binding.aboutBackButtonHitbox.setOnClickListener {
            finish()
        }

        // Open Weblate for translation contributions
        // Translate
        binding.aboutTranslate.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://hosted.weblate.org/engage/opencalc/")
            )
            startActivity(browserIntent)
        }

        // Open Play Store for rating the app
        // Rate
        binding.aboutRate.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.darkempire78.opencalculator")
            )
            startActivity(browserIntent)
        }

        // Donation dialog (currently commented out)
        // Donation
        /*binding.aboutDonate.setOnClickListener {
            DonationDialog(this, layoutInflater).openDonationDialog()
        }*/

        // Open GitHub repository
        // Github
        binding.aboutGithub.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/Darkempire78/OpenCalc")
            )
            startActivity(browserIntent)
        }

        // Open Discord server
        // Discord
        binding.aboutDiscord.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://discord.com/invite/sPvJmY7mcV")
            )
            startActivity(browserIntent)
        }

        // Open privacy policy document
        // Privacy policy
        binding.aboutPrivacyPolicy.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://gist.githubusercontent.com/clementwzk/1688314e8b75d5d32ac0503a97ec77a0/raw/2dcc4cf13f9755405e486e51e4658626c289986a/OpenCalc%2520Privacy%2520Policy.md")
            )
            startActivity(browserIntent)
        }

        // Open license on GitHub
        // License
        binding.aboutLicense.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/Darkempire78/OpenCalc/blob/main/LICENSE")
            )
            startActivity(browserIntent)
        }

        // Easter egg: Show toast message after clicking version 4+ times
        // Easter egg
        var clickAppVersionCount = 0
        binding.aboutAppVersion.setOnClickListener {
            clickAppVersionCount++
            if (clickAppVersionCount > 3) {
                Toast.makeText(this, this.getString(R.string.about_easter_egg), Toast.LENGTH_SHORT).show()
                clickAppVersionCount = 0
            }
        }
    }
}
