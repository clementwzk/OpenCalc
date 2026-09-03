package com.darkempire78.opencalculator;

import static org.junit.Assert.assertEquals;

import com.darkempire78.opencalculator.calculator.Calculator;
import com.darkempire78.opencalculator.calculator.parser.Expression;

import org.junit.BeforeClass;
import org.junit.Test;

import java.text.DecimalFormatSymbols;

public class ScientificNotationTest {

    private static Calculator calc;
    private static Expression expression;
    private static String decimalSeparatorSymbol;
    private static String groupingSeparatorSymbol;

    @BeforeClass
    public static void setup() {
        calc = new Calculator(10);
        expression = new Expression();
        decimalSeparatorSymbol = DecimalFormatSymbols.getInstance().getDecimalSeparator() + "";
        groupingSeparatorSymbol = DecimalFormatSymbols.getInstance().getGroupingSeparator() + "";
    }

    private double calculate(String input) {
        String cleanExpression = expression.getCleanExpression(input, decimalSeparatorSymbol, groupingSeparatorSymbol);
        return calc.evaluate(cleanExpression, false).doubleValue();
    }

    @Test
    public void testScientificNotationPositiveExponent() {
        assertEquals(1000.0, calculate("1e3"), 1e-9);
    }

    @Test
    public void testScientificNotationNegativeExponent() {
        assertEquals(0.025, calculate("2.5e-2"), 1e-9);
    }

    @Test
    public void testScientificNotationSmallNumber() {
        assertEquals(1e-9, calculate("1.0e-9"), 1e-12);
    }

    @Test
    public void testEulerConstant() {
        assertEquals(Math.E, calculate("e"), 1e-9);
    }

    @Test
    public void testEulerConstantPower() {
        assertEquals(Math.pow(Math.E, 2), calculate("e^2"), 1e-9);
    }
}