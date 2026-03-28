package com.darkempire78.opencalculator.calculator.parser

/**
 * Utility object for formatting numbers with locale-specific separators and numbering systems.
 * Supports both International (groups of 3) and Indian (groups of 2 after first 3) numbering systems.
 */
object NumberFormatter {
    /**
     * Formats all numbers in an expression with appropriate digit grouping separators.
     * Preserves non-numeric characters and respects the chosen numbering system.
     * @param text The expression to format
     * @param decimalSeparatorSymbol Locale-specific decimal separator (e.g., "." or ",")
     * @param groupingSeparatorSymbol Digit grouping separator (e.g., "," or " ")
     * @param numberingSystem The numbering system to use (International or Indian)
     * @return Formatted expression with digit grouping applied
     */
    fun format(
        text: String,
        decimalSeparatorSymbol: String,
        groupingSeparatorSymbol: String,
        numberingSystem: NumberingSystem = NumberingSystem.INTERNATIONAL
    ): String {
        // Step 1: Remove existing grouping separators to start fresh
        val textNoSeparator = removeSeparators(text, groupingSeparatorSymbol)
        
        // Step 2: Parse expression into list of numbers and non-numeric elements
        val numbersList = extractString(textNoSeparator, decimalSeparatorSymbol)
        
        // Step 3: Add grouping separators to numbers according to numbering system
        val numbersWithSeparators =
            addSeparators(numbersList, decimalSeparatorSymbol, groupingSeparatorSymbol, numberingSystem)

        // Step 4: Reconstruct the expression from formatted elements
        val newString = StringBuilder()
        for (item in numbersWithSeparators) {
            newString.append(item)
        }

        return newString.toString()
    }

    /**
     * Extracts all elements (numbers and non-numeric characters) from the input string.
     * This function was changed to extract all elements from the input string, not just numbers.
     * This returns a list of all elements. Once the numbers have had separators added, a new string
     * can be constructed from the list.
     * @param text The expression to parse
     * @param decimalSeparatorSymbol The decimal separator to recognize
     * @return List of strings alternating between numbers and non-numeric characters
     */
    private fun extractString(text: String, decimalSeparatorSymbol: String): List<String> {
        val result = mutableListOf<String>()
        var currentNumber = StringBuilder()

        for (char in text) {
            when {
                char.isDigit() || char == decimalSeparatorSymbol.single() -> {
                    currentNumber.append(char)
                }
                else -> {
                    if (currentNumber.isNotEmpty()) {
                        result.add(currentNumber.toString())
                        currentNumber = StringBuilder()
                    }
                    result.add(char.toString())
                }
            }
        }

        if (currentNumber.isNotEmpty()) {
            result.add(currentNumber.toString())
        }

        return result
    }

    /**
     * Adds digit grouping separators to numbers in the list.
     * Only formats the integer part; leaves fractional part unchanged.
     * @param numbersList List of strings (numbers and non-numeric elements)
     * @param decimalSeparatorSymbol Decimal separator to use
     * @param groupingSeparatorSymbol Grouping separator to insert
     * @param numberingSystem The numbering system for grouping rules
     * @return List with formatted numbers
     */
    private fun addSeparators(
        numbersList: List<String>,
        decimalSeparatorSymbol: String,
        groupingSeparatorSymbol: String,
        numberingSystem: NumberingSystem
    ): List<String> {
        return numbersList.map {
            if (it.contains(decimalSeparatorSymbol)) {
                if (it.first() == decimalSeparatorSymbol[0]) {
                    // This means the floating point number doesn't have integers (e.g., ".5")
                    it
                } else {
                    val integersPart = it.substring(0, it.indexOf(decimalSeparatorSymbol))
                    val fractions = it.substring(it.indexOf(decimalSeparatorSymbol) + 1)
                    formatIntegers(
                        integersPart,
                        groupingSeparatorSymbol,
                        numberingSystem == NumberingSystem.INTERNATIONAL
                    ) + decimalSeparatorSymbol + fractions
                }
            } else {
                formatIntegers(it, groupingSeparatorSymbol, numberingSystem == NumberingSystem.INTERNATIONAL)
            }
        }
    }

    /**
     * Formats the integer part of a number with digit grouping.
     * @param integers The integer digits to format
     * @param groupingSeparatorSymbol The separator to insert between groups
     * @param isInternational True for international system (groups of 3), false for Indian system
     * @return Formatted integer string with grouping separators
     */
    private fun formatIntegers(
        integers: String,
        groupingSeparatorSymbol: String,
        isInternational: Boolean
    ): String {
        // sample input  : 00110
        // International system: groups of 3 from right to left
        return if (isInternational) {
            integers.reversed()                         // reversed      : 01100
                .chunked(3)                             // chunked       : [011, 00]
                .joinToString(groupingSeparatorSymbol)  // joinedToString: 011,00
                .reversed()                             // reversed      : 00,110
        } else {
            return formatIndianNumberingSystem(integers)
        }
    }

    /**
     * Removes all grouping separators from the text.
     * @param text The text to process
     * @param groupingSeparatorSymbol The separator to remove
     * @return Text with all grouping separators removed
     */
    private fun removeSeparators(text: String, groupingSeparatorSymbol: String): String {
        return text.replace(groupingSeparatorSymbol, "")
    }

    /**
     * Formats a number using the Indian numbering system.
     * First separator after 3 digits, then every 2 digits.
     * Example: 1234567 -> 12,34,567
     * @param numberStr The number string to format
     * @return Formatted number string with Indian grouping
     */
    private fun formatIndianNumberingSystem(numberStr: String): String {
        // Handle negative numbers
        val isNegative = numberStr.startsWith("-")
        val numberWithoutSign = if (isNegative) numberStr.substring(1) else numberStr

        // Separate integer and decimal parts
        val numberParts = numberWithoutSign.split(".")
        val integerPart = numberParts[0]
        val decimalPart = if (numberParts.size > 1) numberParts[1] else ""

        // Process digits from right to left
        val length = integerPart.length
        val result = StringBuilder()
        var count = 0

        for (i in length - 1 downTo 0) {
            result.append(integerPart[i])
            count++

            when {
                /** First comma comes after 3 digits **/
                count == 3 && i != 0 -> {
                    result.append(',')
                    count = 0
                }
                /** Subsequent commas every 2 digits **/
                count == 2 && i != 0 && length - i > 3 -> {
                    result.append(',')
                    count = 0
                }
            }
        }

        val formattedIntegerPart = result.reverse().toString()
        val formattedNumber = if (decimalPart.isNotEmpty()) "$formattedIntegerPart.$decimalPart" else formattedIntegerPart
        return if (isNegative) "-$formattedNumber" else formattedNumber
    }
}

/**
 * Enum representing different digit grouping systems.
 * - INTERNATIONAL: Groups of 3 digits (e.g., 1,000,000)
 * - INDIAN: First group of 3, then groups of 2 (e.g., 10,00,000)
 */
enum class NumberingSystem(val value: Int, val description: String) {
    INTERNATIONAL(0, "International Numbering System"),
    INDIAN(1, "Indian Numbering System");

    companion object {
        /**
         * Gets the description for a numbering system by its integer value.
         * @param value Integer value (0 for International, 1 for Indian)
         * @return Description string
         */
        fun getDescription(value: Int): String {
            return when (value) {
                0 -> INTERNATIONAL.description
                1 -> INDIAN.description
                else -> INTERNATIONAL.description
            }
        }

        /**
         * Extension function to convert an integer to a NumberingSystem enum.
         * @return Corresponding NumberingSystem (defaults to INTERNATIONAL if invalid)
         */
        fun Int.toNumberingSystem() : NumberingSystem {
            return when (this) {
                0 -> INTERNATIONAL
                1 -> INDIAN
                else -> INTERNATIONAL
            }
        }
    }
}
