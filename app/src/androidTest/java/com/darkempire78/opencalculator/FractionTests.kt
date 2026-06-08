package com.darkempire78.opencalculator

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.darkempire78.opencalculator.calculator.decimalToFraction
import org.junit.Assert.assertEquals
import org.junit.Test

class FractionTests {
    val context: Context = ApplicationProvider.getApplicationContext()
    val view = TextView(context)

    @Test
    fun testFraction(){
        val dec = "4.563"
        decimalToFraction(dec, 1.0E-4, view)
        assertEquals("4 67/119", view.text.toString())
    }

    @Test
    fun testLessThanOneWithZero(){
        val dec = "0.345"
        decimalToFraction(dec, 1.0E-4, view)
        assertEquals("69/200", view.text.toString())
    }

    @Test
    fun testLessThanOneWithoutZero(){
        val dec = ".345"
        decimalToFraction(dec, 1.0E-4, view)
        assertEquals("69/200", view.text.toString())
    }

    @Test
    fun testRepeatDecimalRounding(){
        val dec = "3.33333"
        decimalToFraction(dec, 1.0E-4, view)
        assertEquals("3 1/3", view.text.toString())
    }

    @Test
    fun testRepeatDecimalNoRounding(){
        val dec = "3.33333"
        decimalToFraction(dec, 1.0E-5, view)
        assertEquals("3 33333/100000", view.text.toString())
    }
}