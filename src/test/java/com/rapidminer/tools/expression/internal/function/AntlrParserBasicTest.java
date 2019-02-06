/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.tools.expression.internal.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} from the basic functions block.
 *
 * @author Gisa Schaefer
 *
 */
public class AntlrParserBasicTest extends AntlrParserTest {

	@Test
	public void integerInput() {
		try {
			Expression expression = getExpressionWithoutContext("23643");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(23643d, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void doubleInput() {
		try {
			Expression expression = getExpressionWithoutContext("236.43");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(236.43, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void doubleScientific() {
		try {
			Expression expression = getExpressionWithoutContext("2378423e-10");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(2378423e-10, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void doubleScientificPositive() {
		try {
			Expression expression = getExpressionWithoutContext(".141529e12");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(.141529e12, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void doubleScientificPlus() {
		try {
			Expression expression = getExpressionWithoutContext("3.141529E+12");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(3.141529e12, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void stringInput() {
		try {
			Expression expression = getExpressionWithoutContext("\"bla blup\"");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("bla blup", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void stringWithEscaped() {
		try {
			Expression expression = getExpressionWithoutContext("\"bla\\\"\\\\3\\\" blup\"");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("bla\"\\3\" blup", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void stringWithUnicode() {
		try {
			Expression expression = getExpressionWithoutContext("\"\\u5f3e bla\\u234f blup\\u3333\"");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("\u5f3e bla\u234f blup\u3333", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void stringWithTabsAndNewlines() {
		try {
			Expression expression = getExpressionWithoutContext("\"\\u5f3e bla\nhello\tworld\r\nblup!\"");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("\u5f3e bla hello world blup!", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void multiplyInts() {
		try {
			Expression expression = getExpressionWithFunctionContext("3*5");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(3 * 5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void multiplyDoubles() {
		try {
			Expression expression = getExpressionWithFunctionContext("3.0*5");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(3.0 * 5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void divideInts() {
		try {
			Expression expression = getExpressionWithFunctionContext("4 /2");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(4.0 / 2, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void divideDoubles() {
		try {
			Expression expression = getExpressionWithFunctionContext("5.0 /2");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(5.0 / 2, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void divideByZero() {
		try {
			Expression expression = getExpressionWithFunctionContext("5.0 /0");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void moduloInts() {
		try {
			Expression expression = getExpressionWithFunctionContext("5 %2");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(5 % 2, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void moduloDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("4.7 %1.5");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(0.2, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void differentPointOperations() {
		try {
			Expression expression = getExpressionWithFunctionContext("4%3 *5/2");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(4 % 3 * 5 / 2.0, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void powerInts() {
		try {
			Expression expression = getExpressionWithFunctionContext("2^3^2");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(Math.pow(2, Math.pow(3, 2)), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void powerDoubles() {
		try {
			Expression expression = getExpressionWithFunctionContext("2^3.0^2");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.pow(2, Math.pow(3, 2)), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void stringMultiplication() {
		try {
			getExpressionWithFunctionContext("3* \"blup\"");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void stringDivision() {
		try {
			getExpressionWithFunctionContext("\"blup\" /4");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void unknownFunction() {
		try {
			getExpressionWithFunctionContext("unknown(3)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void powerAsFunctionDoubles() {
		try {
			Expression expression = getExpressionWithFunctionContext("pow(2,0.5)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Math.pow(2, 0.5), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void powerAsFunctionInts() {
		try {
			Expression expression = getExpressionWithFunctionContext("pow (2,3)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(Math.pow(2, 3), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void powerAsFunctionWrongNumberOfArguments() {
		try {
			getExpressionWithFunctionContext("pow(2)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void moduloAsFunctionDoubles() {
		try {
			Expression expression = getExpressionWithFunctionContext("mod(2 ,1.5 )");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(2 % 1.5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void minusOneDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("- 1.5");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(-1.5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void minusDoubles() {
		try {
			Expression expression = getExpressionWithFunctionContext("2- 1.5");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(2 - 1.5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void minusOneInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("- -11");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(11, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void minusInts() {
		try {
			Expression expression = getExpressionWithFunctionContext("-3-12 -11");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(-3 - 12 - 11, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void minusWrong() {
		try {
			getExpressionWithFunctionContext("-3-\"blup\"");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void minusWrongLeft() {
		try {
			getExpressionWithFunctionContext("\"blup\"-5.678");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void minusWrongOne() {
		try {
			getExpressionWithFunctionContext("-\"blup\"");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void plusOneInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("++11");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(11, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusOneDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("+11.06476");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(11.06476, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusOneString() {
		try {
			getExpressionWithFunctionContext("+\"blup\"");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void plusInts() {
		try {
			Expression expression = getExpressionWithFunctionContext("+12+11");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(12 + 11, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusDoubles() {
		try {
			Expression expression = getExpressionWithFunctionContext(".123123+11.06476");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(0.123123 + 11.06476, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusStrings() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"hello \"+\"world\"");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("hello world", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusStringAndDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"hello \"+3.5");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("hello 3.5", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusStringAndMissingDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"hello \"+0/0");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("hello ", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusStringAndInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"hello \"+3");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("hello 3", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusIntAndString() {
		try {
			Expression expression = getExpressionWithFunctionContext("3+\"hello \"");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("3hello ", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusDoubleAndString() {
		try {
			Expression expression = getExpressionWithFunctionContext("3.1415+\"hello \"");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("3.1415hello ", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void plusMissingDoubleAndString() {
		try {
			Expression expression = getExpressionWithFunctionContext("0/0+\"hello \"");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("hello ", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void morePlusDoubleAndString() {
		try {
			Expression expression = getExpressionWithFunctionContext("3.1+3+\"hello \"");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("6.1hello ", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void morePlusStringAndInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"hello \"+3+4");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("hello 34", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void emptyStringAndInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"\"+3");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("3", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void stringAndInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"\"+INFINITY");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("\u221E", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void stringAndMinusInfinity() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"\"+ -INFINITY");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("-\u221E", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeWithSecondNotConstant2() {
		try {
			ExampleSet exampleSet = makeMissingIntegerExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("[integer]+[integer]", resolver);
			resolver.bind(exampleSet.getExample(0));

			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

}
