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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.internal.function.comparison.Equals;
import com.rapidminer.tools.expression.internal.function.comparison.NotEquals;


/**
 * JUnit Tests for the {@link Equals} and {@link NotEquals} functions of the Antlr ExpressionParser
 *
 * @author Sabrina Kirstein
 *
 */
public class AntlrParserEqualsTest extends AntlrParserTest {

	// long value for some date entry
	static long sometime = 1436792411000l;

	// long value for some other date entry
	static long someothertime = 1436792413450l;

	private static ExampleSet makeDateExampleSet() {
		List<Attribute> attributes = new LinkedList<>();

		attributes.add(AttributeFactory.createAttribute("Date", Ontology.DATE_TIME));
		attributes.add(AttributeFactory.createAttribute("Int", Ontology.INTEGER));
		attributes.add(AttributeFactory.createAttribute("otherDate", Ontology.DATE_TIME));

		ExampleSetBuilder builder = ExampleSets.from(attributes);
		double[] data = { sometime, sometime, someothertime };
		builder.addRow(data);
		builder.addRow(data);
		builder.addRow(data);
		builder.addRow(data);

		return builder.build();
	}

	@Test
	public void equalsTrueNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"Moe\" == \"Moe\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsTrueBoolean() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE == TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsTrueDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] == [Date]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsTrueNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("1.45 == 1.45");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsFalseNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"Moe\" == \"Mr.Szyslak\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsFalseBoolean() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE == FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsFalseDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] == [otherDate]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsFalseNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("3.14 == 1");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsTrueNumericNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("3 == \"3\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsFalseNumericNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("3 == \"Claptrap\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsFalseNumericDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("3 == [Date]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsTrueNumericDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type and int type) to compare milliseconds
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] == [Int]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsNumericBooleanTrueTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("1 == TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsNumericBooleanFalseTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("0 == FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsNumericBooleanTrueFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("5 == TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsNumericBooleanFalseFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("-1 == FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsFalseBooleanNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"MoXXi\" == FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsTrueBooleanNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"false\" == FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsBooleanDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("FALSE == [Date]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsNominalDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("\"NOMAD\" == [Date]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsNumericMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("1 == MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("1 == MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("1 == MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("1 == contains(MISSING_NOMINAL,\"test\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsNominalMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"Batman\" == MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("\"Beastmaster\" == MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("\"Phantomas\" == MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("\"Phantomas\" == contains(MISSING_NOMINAL,\"test\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsBoolMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE == MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("FALSE == MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("TRUE == MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("TRUE == contains(MISSING_NOMINAL,\"test\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsDateMissing() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] == MISSING_NUMERIC", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionsAndExamples("[Date] == MISSING_NOMINAL", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionsAndExamples("[Date] == MISSING_DATE", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionsAndExamples("[Date] == contains(MISSING_NOMINAL,\"test\")", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void equalsMissingMissing() {
		try {
			// same type missings
			Expression expression = getExpressionWithFunctionContext("MISSING_NUMERIC == MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_NOMINAL == MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_DATE == MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("contains(MISSING_NOMINAL,\"test\") == contains(MISSING_NOMINAL,\"test\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			// different types missing
			expression = getExpressionWithFunctionContext("MISSING_NUMERIC == MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_NUMERIC == MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_NOMINAL == MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_NOMINAL == contains(MISSING_NOMINAL,\"test\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	// not equals

	@Test
	public void notEqualsFalseNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"Moe\" != \"Moe\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsFalseBoolean() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE != TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsFalseDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] != [Date]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsFalseNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("1.45 != 1.45");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsTrueNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"Moe\" != \"Mr.Szyslak\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsTrueBoolean() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE != FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsTrueDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] != [otherDate]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsTrueNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("3.14 != 1");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsFalseNumericNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("3 != \"3\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsTrueNumericNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("3 != \"Claptrap\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsTrueNumericDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("3 != [Date]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsFalseNumericDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type and int type) to compare milliseconds
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] != [Int]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsNumericBooleanFalseFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("1 != TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsNumericBooleanTrueFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("0 != FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsNumericBooleanFalseTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("5 != TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsNumericBooleanTrueTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("-1 != FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsTrueBooleanNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"MoXXi\" != FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsFalseBooleanNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"false\" != FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsBooleanDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] != FALSE", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsNominalDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] != \"NOMAD\"", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsNumericMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("1 != MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("1 != MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("1 != MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsNominalMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"Batman\" != MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("\"Beastmaster\" != MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("\"Phantomas\" != MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsBoolMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE != MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("FALSE != MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("TRUE != MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("TRUE != contains(MISSING_NOMINAL,\"test\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsDateMissing() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] != MISSING_NUMERIC", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionsAndExamples("[Date]  != MISSING_NOMINAL", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionsAndExamples("[Date]  != MISSING_DATE", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notEqualsMissingMissing() {
		try {
			// same type missings
			Expression expression = getExpressionWithFunctionContext("MISSING_NUMERIC != MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_NOMINAL != MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_DATE != MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("contains(MISSING_NOMINAL,\"test\") != contains(MISSING_NOMINAL,\"test\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
			// different types missing
			expression = getExpressionWithFunctionContext("MISSING_NUMERIC != MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_NUMERIC != MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_NOMINAL != MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
			expression = getExpressionWithFunctionContext("MISSING_NOMINAL != contains(MISSING_NOMINAL,\"test\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}
}
