package com.darkempire78.opencalculator.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.darkempire78.opencalculator.R

/**
 * Dialog for displaying donation options to support the app developer.
 * Provides links to PayPal, Buy Me a Coffee, and GitHub Sponsors.
 * @param context The context used to display the dialog and launch intents
 * @param layoutInflater The layout inflater for creating the dialog view
 */
class DonationDialog (
    private var context: Context,
    private var layoutInflater: LayoutInflater
) {

    /**
     * Displays the donation dialog with clickable donation platform icons.
     * Each icon opens the corresponding donation platform in a browser.
     */
    fun openDonationDialog() {
        // Build the alert dialog
        val dialog = AlertDialog.Builder(context)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_donation, null)
        dialog.setView(dialogView)
        dialog.setTitle(context.getString(R.string.about_dialog_donation_title))

        // Set margins for the dialog content
        val layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(15, 15, 15, 15)
        }
        dialogView.layoutParams = layoutParams

        // Get references to donation platform icons
        val paypalImage = dialogView.findViewById<ImageView>(R.id.paypalImage)
        val bmacImage = dialogView.findViewById<ImageView>(R.id.bmacImage)
        val githubImage = dialogView.findViewById<ImageView>(R.id.githubImage)

        // PayPal donation link
        paypalImage.setOnClickListener {
            val paypalIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/ImDarkempire"))
            context.startActivity(paypalIntent)
        }

        // Buy Me a Coffee donation link
        bmacImage.setOnClickListener {
            val bmacIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/darkempire78"))
            context.startActivity(bmacIntent)
        }

        // GitHub Sponsors donation link
        githubImage.setOnClickListener {
            val bmacIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/Darkempire78"))
            context.startActivity(bmacIntent)
        }

        // Add close button to dismiss the dialog
        dialog.setPositiveButton(R.string.about_dialog_donation_close) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        dialog.show()
    }
}