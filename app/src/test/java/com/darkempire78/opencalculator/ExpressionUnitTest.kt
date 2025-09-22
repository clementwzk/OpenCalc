package com.darkempire78.opencalculator

import com.darkempire78.opencalculator.calculator.Calculator
import com.darkempire78.opencalculator.calculator.parser.Expression
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.text.DecimalFormatSymbols

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExpressionUnitTest {

    private val decimalSeparatorSymbol = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    private val groupingSeparatorSymbol = DecimalFormatSymbols.getInstance().groupingSeparator.toString()
    @Test
    fun percentage_isCorrect() {
        var result = calculate("100*95%", false).toDouble()
        assertEquals(95.0, result, 0.0)

        result = calculate("(10*10)*95%", false).toDouble()
        assertEquals(95.0, result, 0.0)

        result = calculate("100*100%", false).toDouble()
        assertEquals(100.0, result, 0.0)

        result = calculate("100*100/100*100%", false).toDouble()
        assertEquals(100.0, result, 0.0)

        result = calculate("100%10", false).toDouble()
        assertEquals(10.0, result, 0.0)

        result = calculate("10%10%", false).toDouble()
        assertEquals(0.01, result, 0.0)

        result = calculate("900/10%", false).toDouble()
        assertEquals(9000.0, result, 0.0)

        result = calculate("(5+4)%", false).toDouble()
        assertEquals(0.09, result, 0.0)

        result = calculate("5+4%", false).toDouble()
        assertEquals(5.2, result, 0.0)

        result = calculate("(5+4)%", false).toDouble()
        assertEquals(0.09, result, 0.0)

        result = calculate("(50+2)+3%", false).toDouble()
        assertEquals(53.56, result, 0.0)

        result = calculate("52+(1+2)%", false).toDouble()
        assertEquals(53.56, result, 0.0)

        result = calculate("(50+2)+(1+2)%", false).toDouble()
        assertEquals(53.56, result, 0.0)

        result = calculate("164+(265-20%)", isDegreeModeActivated = false).toDouble()
        assertEquals(376.0, result, 0.0)

        result = calculate("(265-20%)+164", isDegreeModeActivated = false).toDouble()
        assertEquals(376.0, result, 0.0)

        result = calculate("164+(265-20%)%", isDegreeModeActivated = false).toDouble()
        assertEquals(511.68, result, 0.0)

        result = calculate("(164+(265-20%)%)+345%", isDegreeModeActivated = false).toDouble()
        assertEquals(2276.976, result, 0.0)

        /*
        Samsung Calculator has a result of 22.76976 here and Google Calculator yields 16.732.
        OpenCalc yields 18.75176. 4Investigating the correct answer.
         We typically would not do something so ambiguous. Are we adding percentages or adding a
         a percentage of the previous number? An answer of 7.21 is also possible. How far down
         this rabbit hole do we want to go? Everyday calculations should be correct now.
         */
        result = calculate("164%+(265-20%)%+345%", isDegreeModeActivated = false).toDouble()
        assertEquals(18.75176, result, 0.0)
    }

    @Test
    fun addition_isCorrect() {
        var result = calculate("1+1", false).toDouble()
        assertEquals(2.0, result, 0.0)

        result = calculate("(1+1)+1", false).toDouble()
        assertEquals(3.0, result, 0.0)
    }


    @Test
    fun nested_factorial_isCorrect() {
        var result = calculate("(3!)!", false).toDouble()
        assertEquals(720.0, result, 0.0)

        result = calculate("(2!)!", false).toDouble()
        assertEquals(2.0, result, 0.0)

        result = calculate("(1!)!", false).toDouble()
        assertEquals(1.0, result, 0.0)
    }



    @Test
    fun number_factorial_equal_decimal_factorial() {
        val number1 = calculate("59!", false).toDouble()
        val decimalNumber1 = calculate("59.0!", false).toDouble()
        assertEquals(number1,decimalNumber1,0.0)

        val number2 = calculate("100!", false).toDouble()
        val decimalNumber2 = calculate("100.0!", false).toDouble()
        assertEquals(number2,decimalNumber2,0.0)

        val number3 = calculate("3004!", false).toDouble()
        val decimalNumber3 = calculate("3004.0!", false).toDouble()
        assertEquals(number3,decimalNumber3,0.0)
    }

    @Test
    fun decimal_factorial_shows_correct_result() {
        val factorial1 = calculate("5.003!", false).toDouble()
        assertEquals(120.6158752971739,factorial1,0.0)

        val factorial2 = calculate("3.01!", false).toDouble()
        assertEquals(6.075928540616668,factorial2,0.0)

        val factorial3 = calculate("7.08!", false).toDouble()
        assertEquals(5924.414931297129,factorial3,0.0)
    }


    @Test
    fun pow_isCorrect() {
        var result = calculate("4^3", false).toDouble()
        assertEquals(64.0, result, 0.0)

        result = calculate("5^-5", false).toDouble()
        assertEquals(0.00032, result, 0.0)

        result = calculate("2^3.5", false).toDouble()
        assertEquals(11.31370849898476, result, 0.0000001)

        result = calculate("2^-3.5", false).toDouble()
        assertEquals(0.08838834764, result, 0.0000001)

        result = calculate("0^2", false).toDouble()
        assertEquals(0.0, result, 0.0)

        result = calculate("2^(2+1)", true).toDouble()
        assertEquals(8.0, result, 0.0)
    }

    @Test
    fun subtraction_isCorrect() {
        var result = calculate("1-1", false).toDouble()
        assertEquals(0.0, result, 0.0)

        result = calculate("1-1-1", false).toDouble()
        assertEquals(-1.0, result, 0.0)
    }

    @Test
    fun division_isCorrect() {
        var result = calculate("0.5/0.01", false).toDouble()
        assertEquals(50.0, result, 0.0)

        result = calculate("7/2", false).toDouble()
        assertEquals(3.5, result, 0.0)
    }

    @Test
    fun factorial_isCorrect() {
        var result = calculate("0!", false).toDouble()
        assertEquals(1.0, result, 0.0)

        result = calculate("5!", false).toDouble()
        assertEquals(120.0, result, 0.0)

        result = calculate("10!", false).toDouble()
        assertEquals(3628800.0, result, 0.0)

        result = calculate("5!+5!", false).toDouble()
        assertEquals(240.0, result, 0.0)

        result = calculate("(3!)!+(3!)!", false).toDouble()
        assertEquals(1440.0, result, 0.0)

        result = calculate("(3!)!/(3!)!", false).toDouble()
        assertEquals(1.0, result, 0.0)
    }

    @Test
    fun factorial_percent_isCorrect() {
        var result = calculate("5!%", false).toDouble()
        assertEquals(1.2, result, 0.0)

        result = calculate("(5!%)+(5!%)", false).toDouble()
        assertEquals(2.4, result, 0.0)

        result = calculate("5!%+5!%", false).toDouble()
        assertEquals(2.64, result, 0.0)
    }

    @Test
    fun sqrt_isCorrect() {
        var result = calculate("√2^2", false).toDouble()
        assertEquals(2.0, result, 0.0)

        result = calculate("√9", false).toDouble()
        assertEquals(3.0, result, 0.0)

        result = calculate("√0.00", false).toDouble()
        assertEquals(0.0, result, 0.0)

        result = calculate("√0.0", false).toDouble()
        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun trigonometric_functions_isCorrect() {
        // In radians
        var result = calculate("cos(0)", false).toDouble()
        assertEquals(1.0, result, 0.0)

        result = calculate("1*cos(0)", false).toDouble()
        assertEquals(1.0, result, 0.0)

        result = calculate("1+cos(0)", false).toDouble()
        assertEquals(2.0, result, 0.0)

        result = calculate("1-cos(0)", false).toDouble()
        assertEquals(0.0, result, 0.0)

        result = calculate("2^cos(0)", false).toDouble()
        assertEquals(2.0, result, 0.0)

        result = calculate("(cos(0))", false).toDouble()
        assertEquals(1.0, result, 0.0)

        result = calculate("cos(2)", false).toDouble()
        assertEquals(-0.4161468365471424, result, 0.0)

        result = calculate("tan(π/2)", false).toDouble()
        assertEquals(0.0, result, 0.0) // 0.0 means that it is impossible

        result = calculate("tan(45)", true).toDouble()
        assertEquals(0.9999999999999999, result, 0.0)

        result = calculate("sin(220)", true).toDouble()
        assertEquals(-0.6427876096865393, result, 0.0)

        result = calculate("sin(5!)", true).toDouble()
        assertEquals(0.8660254037844387, result, 0.0)

        result = calculate("sin(1+1)", true).toDouble()
        assertEquals(0.03489949670250097, result, 0.0)

        result = calculate("1^(1/sin(2π))", true).toDouble()
        assertEquals(1.0, result, 0.0)

        // This should fail but does not as x is assigned 0 during division of zero
        // This causes the evaluation of 1 to the power of 0 which is 1
        result = calculate("1^(1/sin(2π))", false).toDouble()
        assertEquals(1.0, result, 0.0)
    }

    @Test
    fun log2_isCorrect() {
        var result = calculate("log₂(8)", false).toDouble()
        assertEquals(3.0, result, 0.0)

        result = calculate("log₂(5)+5*log(5)", false).toDouble()
        assertEquals(5.816778116567456, result, 0.0)
    }

    @Test
    fun factorial_square_isCorrect() {
        var result = calculate("(√16)!", false).toDouble()
        assertEquals(24.0, result, 0.0)

        result = calculate("(√16)!+(√16)!", false).toDouble()
        assertEquals(48.0, result, 0.0)

        result = calculate("(√16)!+(√16)!+(√16)!", false).toDouble()
        assertEquals(72.0, result, 0.0)
    }

    private fun calculate(input: String, isDegreeModeActivated : Boolean) = calculator.evaluate(expression.getCleanExpression(input, decimalSeparatorSymbol, groupingSeparatorSymbol), isDegreeModeActivated)

    companion object {
        private lateinit var expression: Expression
        private lateinit var calculator: Calculator

        @BeforeClass
        @JvmStatic fun setup() {
            expression = Expression()
            calculator = Calculator(10)
        }
    }
}
