package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.calculator.parser.NumberFormatter
import com.darkempire78.opencalculator.calculator.parser.NumberingSystem
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

/**
 * Unit tests for NumberFormatter functionality.
 *
 * Tests number formatting with grouping separators in:
 * - International numbering system (groups of 3: 1,234,567)
 * - Indian numbering system (first group of 3, then groups of 2: 12,34,567)
 *
 * Covers edge cases like:
 * - Negative numbers
 * - Floating-point numbers
 * - Leading zeros
 * - Already formatted numbers
 * - Empty/zero inputs
 */
class NumberFormatterTest {

    /** Issue #450: Four zeros after comma */
    @Test
    fun `given a decimal expression when formatting then format with grouping separator`() {
        // Given
        val expression = "5,000 + 5,00000"

        // When
        val result = formatter.format(expression, decimalSeparator, groupingSeparator)

        // Then
        val expected = "5,000 + 500,000"
        assertEquals(expected, result)
    }

    @Test
    fun `given a decimal number when formatting then format with grouping separator`() {
        // Given
        val number = "12345"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "12,345"
        assertEquals(expected, result)
    }

    @Test
    fun `given a floating-point number when formatting then format with grouping separator and decimal separator`() {
        // Given
        val number = "1234.567"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "1,234.567"
        assertEquals(expected, result)
    }

    @Test
    fun `given a large number when formatting then format with grouping separator`() {
        // Given
        val number = Long.MAX_VALUE.toString() // 9223372036854775807

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "9,223,372,036,854,775,807"
        assertEquals(expected, result)
    }

    @Test
    fun `given an already formatted number when formatting then keep the same format`() {
        // Given
        val number = "123,456"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "123,456"
        assertEquals(expected, result)
    }

    @Test
    fun `given a number with leading zeros when formatting then keep the leading zeros for decimal`() {
        // Given
        val number = "00100"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "00,100"
        assertEquals(expected, result)
    }

    @Test
    fun `given a number with leading zeros when formatting then keep the leading zeros for floating-point`() {
        // Given
        val number = "0000.0100"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "0,000.0100"
        assertEquals(expected, result)
    }

    @Test
    fun `given a negative integer number when formatting then format with grouping separator and negative sign`() {
        // Given
        val number = "-12345"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "-12,345"
        assertEquals(expected, result)
    }

    @Test
    fun `given a negative floating-point number when formatting then format with grouping separator, decimal separator, and negative sign`() {
        // Given
        val number = "-1234.567"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "-1,234.567"
        assertEquals(expected, result)
    }

    @Test
    fun `given an empty input when formatting then return empty string`() {
        // Given
        val number = ""

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = ""
        assertEquals(expected, result)
    }

    @Test
    fun `given a zero input when formatting then return zero`() {
        // Given
        val number = "0"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "0"
        assertEquals(expected, result)
    }

    @Test
    fun `given a zero input with decimal separator when formatting then return zero with decimal separator`() {
        // Given
        val number = "0.0"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator)

        // Then
        val expected = "0.0"
        assertEquals(expected, result)
    }

    companion object {
        // Shared test configuration for number formatting
        lateinit var decimalSeparator: String
        lateinit var groupingSeparator: String
        lateinit var formatter: NumberFormatter

        /**
         * Sets up test fixtures before any tests run.
         * Uses standard US formatting: "." for decimal, "," for grouping.
         */
        @JvmStatic
        @BeforeClass
        fun setup(): Unit {
            decimalSeparator = "."
            groupingSeparator = ","
            formatter = NumberFormatter
        }
    }

    // Tests for Indian Numbering System formatting

    @Test
    fun `given a floating point number then formatting in Indian Numbering System`() {
        // Given
        val number = "1234567.890"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "12,34,567.890"
        assertEquals(expected, result)
    }

    @Test
    fun `given a large integer number then formatting in Indian Numbering System`() {
        // Given
        val number = "987654321"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "98,76,54,321"
        assertEquals(expected, result)
    }

    @Test
    fun `given a number with two-digit integer part then no formatting is applied`() {
        // Given
        val number = "12.34"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "12.34"
        assertEquals(expected, result)
    }

    @Test
    fun `given a number with exactly three digits then no comma is added`() {
        // Given
        val number = "999"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "999"
        assertEquals(expected, result)
    }

    @Test
    fun `given a number with four digits then a comma is added correctly`() {
        // Given
        val number = "1234"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "1,234"
        assertEquals(expected, result)
    }

    @Test
    fun `given a number with five digits then a comma is added correctly`() {
        // Given
        val number = "12345"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "12,345"
        assertEquals(expected, result)
    }

    @Test
    fun `given a number with six digits then commas are added correctly`() {
        // Given
        val number = "123456"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "1,23,456"
        assertEquals(expected, result)
    }

    @Test
    fun `given a number with decimals then only integer part is formatted`() {
        // Given
        val number = "9020555.555"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "90,20,555.555"
        assertEquals(expected, result)
    }

    @Test
    fun `given a small number then no formatting is applied`() {
        // Given
        val number = "5"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "5"
        assertEquals(expected, result)
    }

    @Test
    fun `given a negative number then formatting is applied correctly`() {
        // Given
        val number = "-1234567"

        // When
        val result = formatter.format(number, decimalSeparator, groupingSeparator, NumberingSystem.INDIAN)

        // Then
        val expected = "-12,34,567"
        assertEquals(expected, result)
    }
}