package com.darkempire78.opencalculator.calculator.parser

import com.darkempire78.opencalculator.calculator.syntax_error

/**
 * Expression parser that transforms user-entered mathematical expressions into
 * a format suitable for evaluation by the calculator engine.
 */
class Expression {

    /**
     * Transforms a user-entered expression into a clean, evaluable format.
     * Handles symbol replacements, implicit multiplication, percentage conversion, etc.
     * @param calculation The raw user input expression
     * @param decimalSeparatorSymbol The decimal separator used in the current locale
     * @param groupingSeparatorSymbol The digit grouping separator (e.g., comma or space)
     * @return A cleaned expression ready for evaluation
     */
    fun getCleanExpression(calculation: String, decimalSeparatorSymbol: String, groupingSeparatorSymbol: String): String {
        // Step 1: Replace user-friendly symbols with calculator-internal representations
        var cleanCalculation = replaceSymbolsFromCalculation(calculation, decimalSeparatorSymbol, groupingSeparatorSymbol)
        
        // Step 2: Add implicit multiplication operators where they're omitted (e.g., 2π -> 2*π)
        cleanCalculation = addMultiply(cleanCalculation)
        
        // Step 3: Convert square root symbol to function notation (√5 -> sqrt(5))
        if (cleanCalculation.contains('√')) {
            cleanCalculation = formatSquare(cleanCalculation)
        }
        
        // Step 4: Process percentage operations with context-aware transformations
        if (cleanCalculation.contains('%')) {
            cleanCalculation = getPercentString(cleanCalculation)
            cleanCalculation = cleanCalculation.replace("%", "/100")
        }
        
        // Step 5: Convert factorial notation to function form (5! -> factorial(5))
        if (cleanCalculation.contains('!')) {
            cleanCalculation = formatFactorial(cleanCalculation)
        }
        
        // Step 6: Auto-close any unclosed parentheses
        cleanCalculation = addParenthesis(cleanCalculation)
        return cleanCalculation
    }

    /**
     * Replaces user-friendly mathematical symbols with calculator-internal representations.
     * @param calculation The expression string to process
     * @param decimalSeparatorSymbol Locale-specific decimal separator
     * @param groupingSeparatorSymbol Locale-specific grouping separator (removed from calculation)
     * @return Expression with standardized symbols
     */
    private fun replaceSymbolsFromCalculation(calculation: String, decimalSeparatorSymbol: String, groupingSeparatorSymbol: String): String {
        // Replace visual multiplication and division symbols with standard operators
        var calculation2 = calculation.replace('×', '*')
        calculation2 = calculation2.replace('÷', '/')
        // Replace logarithm variants with internal function names
        calculation2 = calculation2.replace("log₂(", "logtwo(")
        // Need open parenthesis to prevent alteration of log₂
        calculation2 = calculation2.replace("log(", "logten(")
        
        // Convert scientific notation (e.g., 1.5E3 -> 1.5*10^3)
        calculation2 = calculation2.replace("E", "*10^")
        
        // To avoid that "exp" is interpreted as "e", exp -> xp
        calculation2 = calculation2.replace("exp", "xp")
        
        // To avoid mismatch with cos, sin, tan -> arcco, arcsi, arcta
        calculation2 = calculation2.replace("cos⁻¹", "arcco")
        calculation2 = calculation2.replace("sin⁻¹", "arcsi")
        calculation2 = calculation2.replace("tan⁻¹", "arcta")
        
        // Remove grouping separators and standardize decimal separator to period
        calculation2 = calculation2.replace(groupingSeparatorSymbol, "")
        calculation2 = calculation2.replace(decimalSeparatorSymbol, ".")
        return calculation2
    }

    /**
     * Processes percentage operators with context-aware transformations.
     * Handles three cases:
     * - Standalone: 50% -> 50/100
     * - With * or /: 100*50% -> 100*(50/100)
     * - With + or -: 100+50% -> 100+100*(50/100) (percentage of base)
     * @param calculation Expression containing percentage operators
     * @return Expression with percentages converted to proper mathematical form
     */
    private fun getPercentString(calculation: String): String {
        var result = calculation
        var i = 0
        var parenthesisLevel = 0 // Track nesting level for parentheses
        var subexpressionStart = -1 // Start position of current parenthesized subexpression
        // Track processed % indices to prevent repeated loops
        val processedIndices = mutableSetOf<Int>()
        /*
        New loop to handle nested %. We can probably simply this if we evaluate factorials first as
        it's a unary operator.
         */
        loop@ while (i < result.length) {
            // Check for nested % (Parenthesis first and then %)
            when (result[i]) {
                '(' -> {
                    if (parenthesisLevel == 0) {
                        subexpressionStart = i
                    }
                    parenthesisLevel += 1
                }
                ')' -> {
                    parenthesisLevel -= 1
                    if (parenthesisLevel == 0 && subexpressionStart >= 0) {
                        val subexpression = result.substring(subexpressionStart + 1, i)
                        if (subexpression.contains('%')) {
                            val processedSubexpression = getPercentString(subexpression)
                            result = result.substring(0, subexpressionStart + 1) + processedSubexpression + result.substring(i)
                            i = 0 // Restart to process modified expression
                            processedIndices.clear() // Reset processed indices for new expression
                            continue@loop
                        }
                        subexpressionStart = -1
                    } else if (parenthesisLevel < 0) {
                        syntax_error = true
                        return result
                    }
                }
                '%' -> {
                    if (parenthesisLevel == 0 && !processedIndices.contains(i)) {
                        processedIndices.add(i) // Mark this % as processed
                        // Check if % follows a factorial or closing parenthesis
                        if (i > 0 && result[i - 1] == '!') {
                            // Factorial handler. Find the base before !
                            var j = i - 2
                            while (j >= 0 && (result[j].isDigit() || result[j] == '.')) {
                                j -= 1
                            }
                            val factorialBase = result.substring(j + 1, i - 1)
                            if (factorialBase.isEmpty()) {
                                syntax_error = true
                                return result
                            }
                            val operatorPos = result.substring(0, j + 1).lastIndexOfAny(charArrayOf('+', '-', '*', '/'))
                            if (operatorPos < 0) {
                                // Standalone factorial percentage, e.g., 5!%
                                result = result.substring(0, j + 1) + "(($factorialBase!)/100)" + result.substring(i + 1)
                            } else if (result[operatorPos] == '*' || result[operatorPos] == '/') {
                                // For * or /, wrap the percentage part
                                result = result.substring(0, operatorPos + 1) + "(($factorialBase!)/100)" + result.substring(i + 1)
                            } else {
                                // For + or -, apply percentage relative to the base
                                val base = result.substring(0, operatorPos).trim()
                                if (base.isEmpty()) {
                                    syntax_error = true
                                    return result
                                }
                                result = "$base${result[operatorPos]}$base*(($factorialBase!)/100)" + result.substring(i + 1)
                            }
                        } else if (i > 0 && result[i - 1] == ')') {
                            // Find the matching opening parenthesis
                            var j = i - 2
                            var level = 1
                            while (j >= 0) {
                                if (result[j] == ')') level += 1
                                else if (result[j] == '(') level -= 1
                                if (level == 0) break
                                j -= 1
                            }
                            if (level != 0 || j <= 0) {
                                syntax_error = true
                                return result
                            }
                            val subexpression = result.substring(j + 1, i - 1)
                            val operatorPos = result.substring(0, j).lastIndexOfAny(charArrayOf('+', '-', '*', '/'))
                            if (operatorPos < 0) {
                                result = result.substring(0, j) + "(($subexpression)/100)" + result.substring(i + 1)
                            } else if (result[operatorPos] == '*' || result[operatorPos] == '/') {
                                result = result.substring(0, operatorPos + 1) + "(($subexpression)/100)" + result.substring(i + 1)
                            } else {
                                val base = result.substring(0, operatorPos).trim()
                                if (base.isEmpty()) {
                                    syntax_error = true
                                    return result
                                }
                                result = "$base${result[operatorPos]}$base*(($subexpression)/100)" + result.substring(i + 1)
                            }
                        } else {
                            // Handle regular number before %
                            var start = i - 1
                            while (start >= 0 && (result[start].isDigit() || result[start] == '.')) {
                                start -= 1
                            }
                            val operatorPos = result.substring(0, start + 1).lastIndexOfAny(charArrayOf('+', '-', '*', '/'))
                            if (operatorPos < 0) {
                                val number = result.substring(start + 1, i)
                                if (number.isEmpty()) {
                                    syntax_error = true
                                    return result
                                }
                                result = result.substring(0, start + 1) + "($number/100)" + result.substring(i + 1)
                            } else if (result[operatorPos] == '*' || result[operatorPos] == '/') {
                                val number = result.substring(operatorPos + 1, i)
                                if (number.isEmpty()) {
                                    syntax_error = true
                                    return result
                                }
                                result = result.substring(0, operatorPos + 1) + "($number/100)" + result.substring(i + 1)
                            } else {
                                val base = result.substring(0, operatorPos).trim()
                                val number = result.substring(operatorPos + 1, i)
                                if (base.isEmpty() || number.isEmpty()) {
                                    syntax_error = true
                                    return result
                                }
                                result = "$base${result[operatorPos]}$base*($number/100)" + result.substring(i + 1)
                            }
                        }
                        i = 0
                        continue@loop
                    }
                }
            }
            i += 1
        }

        if (parenthesisLevel != 0) {
            syntax_error = true
        }

        return result
    }

    /**
     * Auto-closes unclosed parentheses and validates parenthesis balance.
     * @param calculation Expression to process
     * @return Expression with balanced parentheses (if possible)
     */
    fun addParenthesis(calculation: String): String {
        // Add ")" which lack
        var cleanCalculation = calculation
        var openParentheses = 0
        var closeParentheses = 0

        for (i in calculation.indices) {
            if (calculation[i] == '(') {
                openParentheses += 1
            }
            if (calculation[i] == ')') {
                closeParentheses += 1
            }
        }
        if (closeParentheses < openParentheses) {
            for (i in 0 until openParentheses - closeParentheses) {
                cleanCalculation += ')'
            }
        }
        // Trigger syntax error if missing '('
        if (closeParentheses > openParentheses) {
            syntax_error = true
        }

        return cleanCalculation
    }

    /**
     * Inserts implicit multiplication operators where they're omitted.
     * Examples: 2π -> 2*π, 3(4) -> 3*(4), 5! -> 5*!, etc.
     * @param calculation Expression to process
     * @return Expression with explicit multiplication operators
     */
    private fun addMultiply(calculation: String): String {
        // Add "*" which lack
        var cleanCalculation = calculation
        var cleanCalculationLength = cleanCalculation.length
        var i = 0
        while (i < cleanCalculationLength) {
            // Case: number/closing paren followed by opening paren (e.g., 2( or )( )
            if (cleanCalculation[i] == '(') {
                if (i != 0 && (cleanCalculation[i-1] in ".e0123456789)")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i)
                    cleanCalculationLength++
                }
            // Case: closing paren followed by number/paren (e.g., )2 or )( )
            } else if (cleanCalculation[i] == ')') {
                if (i + 1 < cleanCalculation.length && cleanCalculation[i + 1] in "0123456789(.") {
                        cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                        cleanCalculationLength++
                    }
            // Case: factorial followed by number/constant/paren (e.g., 5!2 or 5!π)
            } else if (cleanCalculation[i] == '!') {
                if (i + 1 < cleanCalculation.length && (cleanCalculation[i + 1] in "0123456789π(")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                    cleanCalculationLength++
                }
            // Case: percentage followed by number/constant/paren (e.g., 50%2)
            } else if (cleanCalculation[i] == '%') {
                if (i + 1 < cleanCalculation.length && (cleanCalculation[i + 1] in "0123456789π(")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                    cleanCalculationLength++
                }
            // Case: number/symbol before sqrt (e.g., 2√ -> 2*√)
            } else if (i - 1 >= 0 && cleanCalculation[i] == '√') {
                if (cleanCalculation[i - 1] !in "+-/*(") {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i)
                    cleanCalculationLength++
                }
            // Case: pi constant adjacent to other values (e.g., 2π or π3)
            } else if (cleanCalculation[i] == 'π') {
                if (i + 1 < cleanCalculation.length && (cleanCalculation[i + 1] in "0123456789(")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                    cleanCalculationLength++
                }
                if (i - 1 >= 0 && (cleanCalculation[i - 1] in ".%πe0123456789)")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i)
                    cleanCalculationLength++
                }
            // Case: e constant (Euler's number) adjacent to other values (e.g., 2e or e3)
            } else if (cleanCalculation[i] == 'e') {
                if (i + 1 < cleanCalculation.length && (cleanCalculation[i + 1] in "π0123456789(")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                    cleanCalculationLength++
                }
                if (i - 1 >= 0 && (cleanCalculation[i - 1] in ".%πe0123456789)")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i)
                    cleanCalculationLength++
                }
            } else {
                // Case: function names preceded by value (e.g., 2sin -> 2*sin)
                if (i + 1 < cleanCalculation.length) {
                    val functionsList = listOf("arcco", "arcsi", "arcta", "cos", "sin", "tan", "ln", "log", "xp")
                    for (function in functionsList) {
                        val text = cleanCalculation.subSequence(0, i + 1).toString()
                        if (text.endsWith(function) && text.length != function.length) {
                            if (text[text.length - function.length - 1] !in "+-/*(^") {
                                cleanCalculation = cleanCalculation.subSequence(0, i - function.length + 1).toString() +
                                        "*" + function + cleanCalculation.subSequence(i + 1, cleanCalculation.length).toString()
                                cleanCalculationLength++
                                break
                            }
                        }
                    }
                }
            }
            i++
        }
        return cleanCalculation
    }

    /**
     * Converts square root symbol notation to function notation.
     * Example: √5 -> sqrt(5), 2+√16 -> 2+sqrt(16)
     * Automatically adds parentheses around the radicand.
     * @param calculation Expression containing square root symbols
     * @return Expression with sqrt() function notation
     */
    private fun formatSquare(calculation: String): String {
        // Replace √5 by sqrt(5)
        var cleanCalculation = calculation
        var parenthesisOpened = 0

        val cleanCalculationLength = cleanCalculation.length
        var i = 0

        /*
        Changed while (i < cleanCalculationLength)
        as we are modifying the string so length varies
         */
        while (i < cleanCalculation.length) {
            if (i < cleanCalculation.length - 1) {
                if (parenthesisOpened > 0) {
                    if (cleanCalculation[i+1] in "(*-/+^)") {
                        cleanCalculation = cleanCalculation.addCharAtIndex(')', i+1)
                        parenthesisOpened -= 1
                    }
                }
                if (cleanCalculation[i] == '√' && cleanCalculation[i+1] != '(') {
                    cleanCalculation = cleanCalculation.addCharAtIndex('(', i+1)
                    parenthesisOpened += 1
                }
            }
            i++
        }

        cleanCalculation = cleanCalculation.replace("√", "sqrt")
        return cleanCalculation
    }

    /**
     * Converts factorial notation to function notation.
     * Example: 5! -> factorial(5), (3+2)! -> factorial((3+2))
     * Processes from right to left to handle nested factorials correctly.
     * @param calculation Expression containing factorial operators
     * @return Expression with factorial() function notation
     */
    private fun formatFactorial(calculation: String): String {
        var i = calculation.length - 1

        // Return error if the calculation is "!"
        if (i == 0) {
            syntax_error = true
            return ""
        } else {
            var cleanCalculation = calculation

            // Replace 5! by factorial(5)
            while (i > 0) {
                var parenthesisOpened = 0
                // If the current character is "!"
                if (cleanCalculation[i] == '!') {
                    // If the previous character is a parenthesis
                    if (cleanCalculation[i-1] == ')') {
                        // Remove the "!"
                        cleanCalculation = cleanCalculation.substring(0, i) + cleanCalculation.substring(i+1)

                        var j = i
                        while (j > 0) {
                            if (cleanCalculation[j-1] in "*/+^" && parenthesisOpened == 0) {
                                break
                            }
                            if (cleanCalculation[j-1] == ')') parenthesisOpened += 1
                            // If the previous character isn't a parenthesis
                            if (cleanCalculation[j-1] != ')') {
                                // Count open parentheses
                                if (cleanCalculation[j-1] == '(') parenthesisOpened -= 1

                                // If there are no open parentheses, add an F in front of the 1st parenthesis
                                if (parenthesisOpened == 0) {
                                    cleanCalculation = cleanCalculation.addCharAtIndex('F', j-1)
                                    break
                                }
                            }

                            // Decrement i on each run
                            j--
                        }
                    } else {
                        // If the previous character is not a parenthesis, add one
                        cleanCalculation = cleanCalculation.substring(0, i) + ')' + cleanCalculation.substring(i + 1)

                        // Store i in a temporary variable
                        val tmp = i

                        // Run until the previous character is a symbol or parenthesis
                        while (i > 0 && cleanCalculation[i-1] !in "()*-/+^") {
                            // Count open parentheses
                            if (cleanCalculation[i] == ')') parenthesisOpened +=1
                            else if (cleanCalculation[i] == '(') parenthesisOpened -= 1

                            while (i > 1 && (cleanCalculation[i-1].isDigit() || cleanCalculation[i-1] == '.') && cleanCalculation[i-2] !in "()*-/+^") i--
                            // If there is only one parenthesis open, close it and add an F in front of it
                            if (parenthesisOpened == 1) {
                                cleanCalculation = cleanCalculation.addCharAtIndex('(', i-1)
                                cleanCalculation = cleanCalculation.addCharAtIndex('F', i-1)
                            }

                            // Decrement i on each run
                            i--
                        }

                        // Restore i from the temporary variable
                        i = tmp
                    }
                }
                // Decrement i on each run
                i--
            }

            // Replace "F" with "factorial"
            cleanCalculation = cleanCalculation.replace("F", "factorial")

            // Return the final result, so it can be calculated
            return cleanCalculation
        }
    }

    /**
     * Extension function to insert a character at a specific index in a string.
     * @param char The character to insert
     * @param index The position to insert at
     * @return New string with character inserted
     */
    private fun String.addCharAtIndex(char: Char, index: Int) =
        StringBuilder(this).apply { insert(index, char) }.toString()

}
