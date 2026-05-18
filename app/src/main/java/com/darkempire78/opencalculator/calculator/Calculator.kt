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

var division_by_0 = false
var domain_error = false
var syntax_error = false
var is_infinity = false
var require_real_number = false

class Calculator(
        private val numberPrecisionDecimal: Int
    ) {

    fun factorial(number: BigDecimal): BigDecimal {
        if (number >= BigDecimal(3000)) {
            is_infinity = true
            return BigDecimal.ZERO
        }
        return if (number < BigDecimal.ZERO) {
            domain_error = true
            BigDecimal.ZERO
        } else {
            val decimalPartOfNumber = number.toDouble() - number.toInt()
            if (decimalPartOfNumber == 0.0) {
                var factorial = BigInteger("1")
                for (i in 1..number.toInt()) {
                    factorial *= i.toBigInteger()
                }
                factorial.toBigDecimal()
            } else gammaLanczos(number + BigDecimal.ONE)
        }
    }

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
        val g = 7.0
        val z = x.toDouble() - 1.0

        var a = 0.9999999999998099
        for (i in p.indices) {
            a += p[i] / (z + i + 1)
        }

        val t = z + g + 0.5
        val sqrtTwoPi = sqrt(2.0 * PI)
        val firstPart = sqrtTwoPi * t.pow(z + 0.5) * exp(-t)
        val result = firstPart * a

        if (result.isInfinite()) {
            is_infinity = true
            return BigDecimal.ZERO
        }
        return BigDecimal(result, MathContext.DECIMAL64)
    }

    private fun exponentiation(x: BigDecimal, parseFactor: BigDecimal): BigDecimal {
        var value = x
        val intPart = parseFactor.toInt()
        val decimalPart = parseFactor.subtract(BigDecimal(intPart))

        // if the number is null
        if (value == BigDecimal.ZERO) {
            syntax_error = false
            value = BigDecimal.ZERO
        } else {
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
                    value = value.pow(intPart, MathContext.UNLIMITED)
                        .multiply(
                            BigDecimal.valueOf(
                                value.toDouble().pow(decimalPart.toDouble())
                            )
                        )

                    // To fix sqrt(2)^2 = 2
                    val decimal = value.toInt()
                    val fractional = value.toDouble() - decimal
                    if (fractional > 0 && fractional < 1.0E-30) {
                        value = decimal.toBigDecimal()
                    }
                } else {
                    // To support negative factor
                    value = value.pow(-intPart, MathContext.DECIMAL64)
                        .multiply(
                            BigDecimal.valueOf(
                                value.toDouble().pow(-decimalPart.toDouble())
                            )
                        )

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

    fun evaluate(equation: String, isDegreeModeActivated: Boolean): BigDecimal {
        println("Equation BigDecimal : $equation")
        return object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() {
                ch = if (++pos < equation.length) equation[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): BigDecimal {
                nextChar()
                val x = parseExpression()
                if (pos < equation.length) println("Unexpected: \"" + ch.toChar() + "\" in expression: " + equation)
                return x
            }

            fun parseExpression(): BigDecimal {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x = x.add(parseTerm()) // addition
                    else if (eat('-'.code)) x = x.subtract(parseTerm()) // subtraction
                    else return x
                }
            }

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
                    if (string.count { it == '.' } > 1) {
                        x = BigDecimal.ZERO
                        syntax_error = true
                    } else {
                        if ((string.length == 1) && (string[0] == '.')) {
                            x = BigDecimal.ZERO
                            syntax_error = true
                        } else {
                            x = BigDecimal(string)
                        }
                    }
                } else if (eat('e'.code)) {
                    x = BigDecimal(Math.E)
                } else if (eat('π'.code)) {
                    x = BigDecimal(PI)
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func: String = equation.substring(startPos, pos)
                    if (eat('('.code)) {
                        x = parseExpression()
                        if (!eat(')'.code)) x = parseFactor()
                    } else {
                        x = parseFactor()
                    }
                    println(x)
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
                                if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                    x = round(x.toDouble()).toBigDecimal()
                                }
                            }
                        }
                        "arcsi" -> {
                            if (abs(x.toDouble()) > 1) {
                                x = BigDecimal.ZERO
                                domain_error = true
                            } else {
                                x = if (isDegreeModeActivated) {
                                    (asin(x.toDouble()) * 180 / Math.PI).toBigDecimal()
                                } else {
                                    asin(x.toDouble()).toBigDecimal()
                                }
                                if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                    x = round(x.toDouble()).toBigDecimal()
                                }
                            }
                        }
                        "arcco" -> {
                            if (abs(x.toDouble()) > 1) {
                                x = BigDecimal.ZERO
                                domain_error = true
                            } else {
                                x = if (isDegreeModeActivated) {
                                    (acos(x.toDouble())*180/Math.PI).toBigDecimal()
                                } else {
                                    acos(x.toDouble()).toBigDecimal()
                                }
                                if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                    x = round(x.toDouble()).toBigDecimal()
                                }
                            }

                        }
                        "arcta" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if  (isDegreeModeActivated) {
                                x = (atan(x.toDouble()) * 180 / Math.PI).toBigDecimal()
                            } else {
                                x =atan(x.toDouble()).toBigDecimal()
                            }
                            if (x > BigDecimal.ZERO && x < BigDecimal(1.0E-14)) {
                                x = round(x.toDouble()).toBigDecimal()
                            }
                        }
                        else -> {
                            syntax_error = true
                        }
                    }
                } else {
                    x = BigDecimal.ZERO
                    syntax_error = true
                }
                if (eat('^'.code)) {
                    x = exponentiation(x, parseFactor())
                }
                return x
            }
        }.parse()
    }
}
