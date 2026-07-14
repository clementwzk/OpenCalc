package com.darkempire78.opencalculator

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.util.TypedValue
import android.widget.TextView

/**
 * Utility class for dynamically adjusting text size to fit within screen width.
 * Scales text between min/max sizes based on screen orientation and size category.
 */
class TextSizeAdjuster(private val context: Context) {

    /**
     * Enum specifying which text field is being adjusted.
     * Different min/max sizes are used for input vs output displays.
     */
    enum class AdjustableTextType {
        Input,  // Main input/calculation field
        Output, // Result display field
    }

    /**
     * Dynamically adjusts the text size of a TextView to fit within screen width.
     * Iteratively reduces size from max to min until text fits.
     * @param textView The TextView to adjust
     * @param adjustableTextType Whether this is Input or Output text
     */
    fun adjustTextSize(textView: TextView, adjustableTextType: AdjustableTextType) {
        val screenWidth = context.resources.displayMetrics.widthPixels

        // Text size will be reduced a bit before reaching the screen width, for a smoother experience
        val maxWidth = screenWidth - dpToPx(25f)

        // Get the min and max text sizes
        val (minTextSize, maxTextSize) = getTextSizeBounds(context.resources.configuration, adjustableTextType)

        var textSize = maxTextSize
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)

        val textBounds = Rect()
        val text = textView.text.toString()

        // Measure the text size
        val paint = textView.paint
        paint.getTextBounds(text, 0, text.length, textBounds)

        // Reduce the text size until it fits
        while (textBounds.width() > maxWidth && textSize > minTextSize) {
            textSize -= 1f
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            paint.getTextBounds(text, 0, text.length, textBounds)
        }
    }

    /**
     * Determines min and max text sizes based on device configuration.
     * @param configuration Device configuration (orientation, screen size)
     * @param adjustableTextType Whether this is Input or Output text
     * @return Pair of (minSize, maxSize) in SP units
     */
    private fun getTextSizeBounds(configuration: Configuration, adjustableTextType: AdjustableTextType): Pair<Float, Float> {
        val orientation = configuration.orientation
        val screenSize = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

        val (minTextSize, maxTextSize) = if (adjustableTextType == AdjustableTextType.Input) {
            getInputTextSizeBounds(orientation, screenSize)
        } else {
            getResultTextSizeBounds(orientation, screenSize)
        }

        return Pair(minTextSize, maxTextSize)
    }

    /**
     * Gets text size bounds specifically for the input field.
     * Larger screens in landscape get bigger text sizes.
     * @return Pair of (minSize, maxSize) in SP units
     */
    private fun getInputTextSizeBounds(orientation: Int, screenSize: Int): Pair<Float, Float> {
        return when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> Pair(35f, 55f)
            Configuration.ORIENTATION_LANDSCAPE -> {
                when (screenSize) {
                    Configuration.SCREENLAYOUT_SIZE_SMALL -> Pair(35f, 55f)
                    Configuration.SCREENLAYOUT_SIZE_NORMAL -> Pair(35f, 55f)
                    Configuration.SCREENLAYOUT_SIZE_LARGE -> Pair(55f, 95f)
                    Configuration.SCREENLAYOUT_SIZE_XLARGE -> Pair(55f, 95f)
                    else -> Pair(35f, 55f)
                }
            }
            Configuration.ORIENTATION_UNDEFINED -> {
                println("❌ Undefined orientation : screenSize -> $screenSize orientation -> $orientation")
                Pair(0f, 0f)
            }
            else -> {
                println("❌ Undefined orientation (else) : screenSize -> $screenSize orientation -> $orientation")
                Pair(0f, 0f)
            }
        }
    }

    /**
     * Gets text size bounds specifically for the result/output field.
     * Generally smaller than input field sizes.
     * @return Pair of (minSize, maxSize) in SP units
     */
    private fun getResultTextSizeBounds(orientation: Int, screenSize: Int): Pair<Float, Float> {
        return when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> Pair(25f, 40f)
            Configuration.ORIENTATION_LANDSCAPE -> {
                when (screenSize) {
                    Configuration.SCREENLAYOUT_SIZE_SMALL -> Pair(20f, 30f)
                    Configuration.SCREENLAYOUT_SIZE_NORMAL -> Pair(20f, 30f)
                    Configuration.SCREENLAYOUT_SIZE_LARGE -> Pair(25f, 45f)
                    Configuration.SCREENLAYOUT_SIZE_XLARGE -> Pair(25f, 45f)
                    else -> Pair(20f, 30f)
                }
            }
            Configuration.ORIENTATION_UNDEFINED -> {
                println("❌ Undefined orientation : screenSize -> $screenSize orientation -> $orientation")
                Pair(0f, 0f)
            }
            else -> {
                println("❌ Undefined orientation (else) : screenSize -> $screenSize orientation -> $orientation")
                Pair(0f, 0f)
            }
        }
    }

    /**
     * Converts density-independent pixels (DP) to actual pixels (PX).
     * @param dp Value in DP units
     * @return Equivalent value in PX units
     */
    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}