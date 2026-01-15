package com.darkempire78.opencalculator.calculator

import android.os.Build
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// Global error flags used to track calculation errors
var division_by_0 = false // Triggered when attempting to divide by zero
var domain_error = false // Triggered when input is outside a function's valid domain (e.g., ln of negative number)
var syntax_error = false // Triggered when encountering invalid expression syntax
var is_infinity = false // Triggered when result exceeds representable range
var require_real_number = false // Triggered when result would be complex/imaginary (e.g., sqrt of negative number)

/**
 * Calculator class that evaluates mathematical expressions using BigDecimal for high precision.
 * @param numberPrecisionDecimal The number of decimal places to use for non-terminating division operations
 */
class Calculator(
        private val numberPrecisionDecimal: Int
    ) {

    /**
     * Computes the factorial of a number (n!).
     * For integers, uses iterative calculation. For non-integers, uses Gamma function approximation.
     * @param number The number to calculate factorial for
     * @return The factorial result, or zero if error occurs
     */
    fun factorial(number: BigDecimal): BigDecimal {
        // Prevent overflow: factorials above 3000 are too large to compute
        if (number >= BigDecimal(3000)) {
            is_infinity = true
            return BigDecimal.ZERO
        }
        // Factorial is undefined for negative numbers
        return if (number < BigDecimal.ZERO) {
            domain_error = true
            BigDecimal.ZERO
        } else {
            val decimalPartOfNumber = number.toDouble() - number.toInt()
            if (decimalPartOfNumber == 0.0) {
                // Standard factorial calculation for integers: n! = 1 × 2 × 3 × ... × n
                var factorial = BigInteger("1")
                for (i in 1..number.toInt()) {
                    factorial *= i.toBigInteger()
                }
                factorial.toBigDecimal()
            } else gammaLanczos(number + BigDecimal.ONE) // For non-integers: use Gamma function where Γ(n+1) = n!
        }
    }

    /**
     * Computes the Gamma function using Lanczos approximation.
     * The Gamma function extends factorial to real and complex numbers: Γ(n) = (n-1)!
     * @param x The input value
     * @return The Gamma function result
     */
    private fun gammaLanczos(x: BigDecimal): BigDecimal {
        // Lanczos approximation parameters
        val p = arrayOf(
            676.5203681218851,
            -1259.1392167224028,
            771.3234287776531,
            -176.6150291621406,
            12.507343278686905,
            -0.13857109526572012,
            9.984369578019572e-6,
            1.5056327351493116e-7
        )
        val g = 7.0 // Lanczos constant
        val z = x.toDouble() - 1.0

        // Calculate the series approximation using Lanczos coefficients
        var a = 0.9999999999998099
        for (i in p.indices) {
            a += p[i] / (z + i + 1)
        }

        // Apply Lanczos formula: Γ(x) = √(2π) × t^(z+0.5) × e^(-t) × a
        val t = z + g + 0.5
        val sqrtTwoPi = sqrt(2.0 * PI)
        val firstPart = sqrtTwoPi * t.pow(z + 0.5) * exp(-t)
        val result = firstPart * a

        return BigDecimal(result, MathContext.DECIMAL64)
    }

    /**
     * Computes exponentiation (base^exponent) with support for decimal exponents.
     * Handles special cases like negative bases with fractional exponents and negative exponents.
     * @param x The base value
     * @param parseFactor The exponent value
     * @return The result of x raised to the power of parseFactor
     */
    private fun exponentiation(x: BigDecimal, parseFactor: BigDecimal): BigDecimal {
        var value = x
        // Split exponent into integer and decimal parts for separate processing
        val intPart = parseFactor.toInt()
        val decimalPart = parseFactor.subtract(BigDecimal(intPart))

        // if the number is null
        if (value == BigDecimal.ZERO) {
            syntax_error = false
            value = BigDecimal.ZERO
        } else {
            // Prevent overflow for extremely large exponents
            if (parseFactor > BigDecimal(10000)) {
                is_infinity = true
                value = BigDecimal.ZERO
            } else {
                // If the number is negative and the factor is a float ( e.g : (-5)^0.5 )
                if (value < BigDecimal.ZERO && decimalPart != BigDecimal.ZERO) {
                    require_real_number = true
                } // the factor is NOT a float
                else if (parseFactor > BigDecimal.ZERO) {

                    // To support bigdecimal exponent (e.g: 3.5)
                    // Split calculation: x^(a+b) = x^a × x^b where a is integer part, b is decimal part
                    value = value.pow(intPart, MathContext.UNLIMITED)
                        .multiply(
                            BigDecimal.valueOf(
                                value.toDouble().pow(decimalPart.toDouble())
                            )
                        )

                    // To fix sqrt(2)^2 = 2
                    // Round very small fractional parts to nearest integer to avoid floating point errors
                    val decimal = value.toInt()
                    val fractional = value.toDouble() - decimal
                    if (fractional > 0 && fractional < 1.0E-30) {
                        value = decimal.toBigDecimal()
                    }
                } else {
                    // To support negative factor
                    // Calculate positive power first, then take reciprocal: x^(-n) = 1 / x^n
                    value = value.pow(-intPart, MathContext.DECIMAL64)
                        .multiply(
                            BigDecimal.valueOf(
                                value.toDouble().pow(-decimalPart.toDouble())
                            )
                        )

                    // Take reciprocal to handle negative exponent
                    value = try {
                        BigDecimal.ONE.divide(value)
                    } catch (e: ArithmeticException) {
                        // if the result is a non-terminating decimal expansion
                        BigDecimal.ONE.divide(value, numberPrecisionDecimal, RoundingMode.HALF_DOWN)
                    }
                }
            }
        }
        return value
    }

    fun bigDecimalSqrtFormerAndroidVersion(value: BigDecimal, mathContext: MathContext): BigDecimal {
        // Newton's method for square root calculation with Android versions prior to API 33
        var x0 = BigDecimal(0)
        var x1 = value.divide(BigDecimal(2), mathContext)

        // != evaluated true when comparing 0 and 0.0
        // This allowed the passing of 0.0 (or more trailing zeroes) to be divided.
        while (x0 < x1 || x0 > x1) {
            x0 = x1
            x1 = value.divide(x0, mathContext).add(x0).divide(BigDecimal(2), mathContext)
        }

        return x1
    }

    /**
     * Evaluates a mathematical expression string and returns the result.
     * Uses recursive descent parsing to handle operator precedence correctly.
     * @param equation The mathematical expression to evaluate (e.g., "2+3*4")
     * @param isDegreeModeActivated Whether trigonometric functions should use degrees (true) or radians (false)
     * @return The calculated result as a BigDecimal
     */
    fun evaluate(equation: String, isDegreeModeActivated: Boolean): BigDecimal {
        println("Equation BigDecimal : $equation")
        // Anonymous inner class implementing recursive descent parser
        return object : Any() {
            var pos = -1 // Current position in the equation string
            var ch = 0 // Current character being processed (as ASCII code)
            
            // Advance to the next character in the equation
            fun nextChar() {
                ch = if (++pos < equation.length) equation[pos].code else -1
            }

            // Check if current character matches expected character and consume it if so
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar() // Skip whitespace
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            // Entry point for parsing: starts the recursive descent parser
            fun parse(): BigDecimal {
                nextChar()
                val x = parseExpression()
                if (pos < equation.length) println("Unexpected: \"" + ch.toChar() + "\" in expression: " + equation)
                return x
            }

            // Parse addition and subtraction (lowest precedence)
            fun parseExpression(): BigDecimal {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x = x.add(parseTerm()) // addition
                    else if (eat('-'.code)) x = x.subtract(parseTerm()) // subtraction
                    else return x
                }
            }

            // Parse multiplication, division, and modulo (higher precedence than addition/subtraction)
            fun parseTerm(): BigDecimal {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x = x.multiply(parseFactor()) // Multiplication
                    else if (eat('#'.code)) { // Modulo
                        val fractionDenominator = parseFactor()
                        if (fractionDenominator == BigDecimal.ZERO) {
                            division_by_0 = true
                            x = BigDecimal.ZERO
                        } else {
                            x = x.rem(fractionDenominator)
                        }
                    }
                    else if (eat('/'.code)) { // Division
                        val fractionDenominator = parseFactor()
                        // The Double value is the result of sin(2π) in Radian mode after conversions (0)
                        // This catches the error/crash during zero division in issue #499
                        if (fractionDenominator.toFloat() == 0f || fractionDenominator.toDouble() == -2.4492935982947064E-16) {
                            division_by_0 = true
                            x = BigDecimal.ZERO
                        } else {
                            try {
                                x = x.divide(fractionDenominator)
                            } catch (e: ArithmeticException) { // if the result is a non-terminating decimal expansion
                                x = x.divide(fractionDenominator, numberPrecisionDecimal, RoundingMode.HALF_DOWN)
                                println(x)
                            }
                        }
                    }
                    else return x
                }
            }

            // Parse numbers, functions, parentheses, and exponentiation (highest precedence)
            fun parseFactor(): BigDecimal {
                if (eat('+'.code)) return parseFactor().plus() // unary plus
                if (eat('-'.code)) return parseFactor().unaryMinus() // unary minus
                var x: BigDecimal
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    if (!eat(')'.code)) {
                        println("Missing ')'")
                        x = BigDecimal.ZERO
                        syntax_error = true
                    }
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    val string = equation.substring(startPos, pos)
                    // Validate number format
                    if (string.count { it == '.' } > 1) {
                        // Multiple decimal points is invalid
                        x = BigDecimal.ZERO
                        syntax_error = true
                    } else {
                        if ((string.length == 1) && (string[0] == '.')) {
                            // A lone decimal point is invalid
                            x = BigDecimal.ZERO
                            syntax_error = true
                        } else {
                            x = BigDecimal(string)
                        }
                    }
                } else if (eat('e'.code)) {
                    // Euler's number constant
                    x = BigDecimal(Math.E)
                } else if (eat('π'.code)) {
                    // Pi constant
                    x = BigDecimal(PI)
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    // Parse function name
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func: String = equation.substring(startPos, pos)
                    // Parse function argument (with or without parentheses)
                    if (eat('('.code)) {
                        x = parseExpression()
                        if (!eat(')'.code)) x = parseFactor()
                    } else {
                        x = parseFactor()
                    }
                    println(x)
                    // Process function based on name
                    when (func) {
                        "sqrt" -> {
                            if (x >= BigDecimal.ZERO) {
                                // Set the precision for the square root calculation
                                val integerPartLength = x.toString().length
                                val maxPrecision = (integerPartLength + 50).coerceAtMost(1000) // Maximum precision is 1000
                                val precision = MathContext(maxPrecision, RoundingMode.HALF_DOWN)
                                x = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Use default BigDecimal sqrt function (API 33)
                                    x.sqrt(precision)
                                } else { // Use Newton's method for square root calculation with Android versions prior to API 33
                                    bigDecimalSqrtFormerAndroidVersion(x, precision)
                                }
                            } else {
                                require_real_number = true
                            }

                        }
                        "factorial" -> {
                            x = factorial(x)
                        }
                        "ln" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (x <= BigDecimal.ZERO) {
                                domain_error = true
                            } else {
                                x = BigDecimal(ln(x.toDouble()))
                            }
                        }
                        "logtwo" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (x <= BigDecimal.ZERO) {
                                domain_error = true
                            } else {
                                x = BigDecimal(log2(x.toDouble()))
                            }
                        }
                        "logten" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (x <= BigDecimal.ZERO) {
                                domain_error = true
                            } else {
                                x = BigDecimal(log10(x.toDouble()))
                            }
                        }
                        "xp" -> {
                            x = exponentiation(BigDecimal(Math.E), x)
                        }
                        "sin" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (isDegreeModeActivated) {
                                x = sin(Math.toRadians(x.toDouble())).toBigDecimal()
                                // https://stackoverflow.com/questions/29516222/how-to-get-exact-value-of-trigonometric-functions-in-java
                            } else {
                                x = sin(x.toDouble()).toBigDecimal()
                            }
                            // Round very small values to zero to handle floating point precision issues
                            if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                x = round(x.toDouble()).toBigDecimal()
                            }
                        }
                        "cos" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (isDegreeModeActivated) {
                                x = cos(Math.toRadians(x.toDouble())).toBigDecimal()
                            } else {
                                x = cos(x.toDouble()).toBigDecimal()
                            }
                            // Round very small values to zero to handle floating point precision issues
                            if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                x = round(x.toDouble()).toBigDecimal()
                            }
                        }
                        "tan" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (Math.toDegrees(x.toDouble()) == 90.0) {
                                // Tangent is defined for R\{(2k+1)π/2, with k ∈ Z}
                                domain_error = true
                                x = BigDecimal.ZERO
                            } else {
                                x = if (isDegreeModeActivated) {
                                    tan(Math.toRadians(x.toDouble())).toBigDecimal()
                                } else {
                                    tan(x.toDouble()).toBigDecimal()
                                }
                                // Round very small values to zero to handle floating point precision issues
                                if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                    x = round(x.toDouble()).toBigDecimal()
                                }
                            }
                        }
                        "arcsi" -> { // Inverse sine (arcsin or sin^-1)
                            // arcsin is only defined for inputs in [-1, 1]
                            if (abs(x.toDouble()) > 1) {
                                x = BigDecimal.ZERO
                                domain_error = true
                            } else {
                                x = if (isDegreeModeActivated) {
                                    (asin(x.toDouble()) * 180 / Math.PI).toBigDecimal()
                                } else {
                                    asin(x.toDouble()).toBigDecimal()
                                }
                                // Round very small values to zero to handle floating point precision issues
                                if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                    x = round(x.toDouble()).toBigDecimal()
                                }
                            }
                        }
                        "arcco" -> { // Inverse cosine (arccos or cos^-1)
                            // arccos is only defined for inputs in [-1, 1]
                            if (abs(x.toDouble()) > 1) {
                                x = BigDecimal.ZERO
                                domain_error = true
                            } else {
                                x = if (isDegreeModeActivated) {
                                    (acos(x.toDouble())*180/Math.PI).toBigDecimal()
                                } else {
                                    acos(x.toDouble()).toBigDecimal()
                                }
                                // Round very small values to zero to handle floating point precision issues
                                if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                    x = round(x.toDouble()).toBigDecimal()
                                }
                            }

                        }
                        "arcta" -> { // Inverse tangent (arctan or tan^-1)
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if  (isDegreeModeActivated) {
                                x = (atan(x.toDouble()) * 180 / Math.PI).toBigDecimal()
                            } else {
                                x =atan(x.toDouble()).toBigDecimal()
                            }
                            // Round very small values to zero to handle floating point precision issues
                            if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                x = round(x.toDouble()).toBigDecimal()
                            }
                        }
                        else -> {
                            // Unknown function name
                            syntax_error = true
                        }
                    }
                } else {
                    // Invalid token: not a number, constant, function, or parenthesis
                    x = BigDecimal.ZERO
                    syntax_error = true
                }
                // Handle exponentiation operator (highest precedence, right-associative)
                if (eat('^'.code)) {
                    x = exponentiation(x, parseFactor())
                }
                return x
            }
        }.parse()
    }
}
