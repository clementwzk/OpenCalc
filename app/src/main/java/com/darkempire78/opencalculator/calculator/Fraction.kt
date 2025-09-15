package com.darkempire78.opencalculator.calculator

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.widget.TextView
import com.darkempire78.opencalculator.calculator.parser.NumberFormatter
import kotlin.math.abs
import kotlin.math.floor

fun decimalToFraction(calculation: String, precision: Double, textView: TextView) {

    val decimalPosition = calculation.indexOf('.')
    val decimal = calculation.substring(decimalPosition).toDouble()
    val whole = if (decimalPosition > 0) {
        NumberFormatter.format(
            calculation.substring(0, decimalPosition),
            ".",
            ","
        )

    } else ""

    var n1 = 1
    var n2 = 0
    var d1 = 0
    var d2 = 1

    var dec = decimal // - base.toDouble()
    val dec2 = decimal

    do {
        val a = floor(dec).toInt()
        val numer = n1
        val denom = d1
        n1 = a * n1 + n2
        n2 = numer
        d1 = a * d1 + d2
        d2 = denom
        dec = 1 / (dec - a)
    } while (abs(dec2 - n1.toDouble() / d1) > dec2 * precision)

    if (whole != "") {
        if (n1 == 0) {
            textView.text = whole.toString()
        } else {
            val tempString = "$whole $n1/$d1"
            val stringSpan = SpannableStringBuilder(tempString)
            val spaceLoc = stringSpan.indexOf(' ')
            val stringLen = stringSpan.length
            stringSpan.setSpan(
                SuperscriptSpan(),
                spaceLoc,
                stringLen,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            stringSpan.setSpan(
                RelativeSizeSpan(0.6f),
                spaceLoc,
                stringLen,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textView.text = stringSpan
        }
    } else {
        val tempString = "$n1/$d1"
        val stringSpan = SpannableStringBuilder(tempString)
        textView.text = stringSpan
    }
}