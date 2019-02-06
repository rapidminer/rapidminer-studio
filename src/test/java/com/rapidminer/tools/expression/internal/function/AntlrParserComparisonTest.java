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
import static org.junit.Assert.assertNotNull;
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


/**
 * JUnit Tests for the comparison functions of the Antlr ExpressionParser
 *
 * @author Thilo Kamradt
 *
 */
public class AntlrParserComparisonTest extends AntlrParserTest {

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

	// missing

	@Test
	public void missingTrueNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing(MISSING_NOMINAL)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void missingTrueNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing(MISSING_NUMERIC)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void missingTrueDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing(MISSING_DATE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void missingTrueBinominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing(contains(MISSING_NOMINAL,\"test\"))");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void missingNoArg() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing()");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void missingTooManyArgs() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing(1,2,3,4)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void missingFalseNumber() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing(1)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void missingFalseNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing(\"HandsomeJack\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void missingFalseNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing(pi)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void missingFalseBoolean() {
		try {
			Expression expression = getExpressionWithFunctionContext("missing(TRUE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void missingFalseDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("missing([Date])", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	// <
	@Test
	public void smallerTrueNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("1 < 1.2");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerFalseNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("1.2 < 1.2");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerTrueNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"bo\" < \"ca\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerFalseNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" < \"baboom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerNominalMissingFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" < MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerNominalMissingError() {
		try {
			Expression expression = getExpressionWithFunctionContext(" MISSING_NOMINAL < 8");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerNumericMissingFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("5 < MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerNumericMissingError() {
		try {
			Expression expression = getExpressionWithFunctionContext(" MISSING_NUMERIC < \"boom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerDateMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" < MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerDifferentTypes() {
		try {
			Expression expression = getExpressionWithFunctionContext("8 < \"baboom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerBool() {
		try {
			Expression expression = getExpressionWithFunctionContext("FALSE < TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerNumericBool() {
		try {
			Expression expression = getExpressionWithFunctionContext("0 < TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] < [otherDate]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// <=

	@Test
	public void smallerEqualTrueNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("1.2 <= 1.2");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerEqualFalseNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("2 <= 1.2");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerEqualTrueNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"bo\" <= \"bo\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerEqualFalseNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" <= \"baboom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerEqualNominalMissingFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" <= MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerEqualNominalMissingError() {
		try {
			Expression expression = getExpressionWithFunctionContext(" MISSING_NOMINAL <= 8");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerEqualNumericMissingFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("5 <= MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void smallerEqualNumericMissingError() {
		try {
			Expression expression = getExpressionWithFunctionContext(" MISSING_NUMERIC <= \"boom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerEqualDateMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" <= MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerEqualDifferentTypes() {
		try {
			Expression expression = getExpressionWithFunctionContext("8 <= \"baboom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerEqualBool() {
		try {
			Expression expression = getExpressionWithFunctionContext("FALSE <= TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerEqualNumericBool() {
		try {
			Expression expression = getExpressionWithFunctionContext("0 <= TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void smallerEqualDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] <= [otherDate]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// >

	@Test
	public void greaterTrueNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("1.2 > 1");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterFalseNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("1.2 > 1.2");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterTrueNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"ca\" > \"bc\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterFalseNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"baboom\" > \"boom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterNominalMissingFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" > MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterNominalMissingError() {
		try {
			Expression expression = getExpressionWithFunctionContext(" MISSING_NOMINAL > 8");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterNumericMissingFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("5 > MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterNumericMissingError() {
		try {
			Expression expression = getExpressionWithFunctionContext(" MISSING_NUMERIC > \"boom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterDateMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" > MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterDifferentTypes() {
		try {
			Expression expression = getExpressionWithFunctionContext("8 > \"baboom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterBool() {
		try {
			Expression expression = getExpressionWithFunctionContext("FALSE > TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterNumericBool() {
		try {
			Expression expression = getExpressionWithFunctionContext("0 > TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] > [otherDate]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// >=

	@Test
	public void greaterEqualTrueNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("1.2 >= 1.2");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterEqualFalseNumeric() {
		try {
			Expression expression = getExpressionWithFunctionContext("1.2 >= 2");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterEqualTrueNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"ca\" >= \"bc\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterEqualFalseNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"baboom\" >= \"boom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterEqualNominalMissingFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" >= MISSING_NOMINAL");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterEqualNominalMissingError() {
		try {
			Expression expression = getExpressionWithFunctionContext(" MISSING_NOMINAL >= 8");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterEqualNumericMissingFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("5 >= MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void greaterEqualNumericMissingError() {
		try {
			Expression expression = getExpressionWithFunctionContext(" MISSING_NUMERIC >= \"boom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterEqualDateMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("\"boom\" >= MISSING_DATE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterEqualDifferentTypes() {
		try {
			Expression expression = getExpressionWithFunctionContext("8 >= \"baboom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterEqualBool() {
		try {
			Expression expression = getExpressionWithFunctionContext("FALSE >= TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterEqualNumericBool() {
		try {
			Expression expression = getExpressionWithFunctionContext("0 >= TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void greaterEqualDate() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("[Date] >= [otherDate]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// isFinite() tests

	@Test
	public void isFiniteTrueInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite(234)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void isFiniteTrueDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite(234.567)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void isFiniteFalseInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite(INFINITY)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void isFiniteFalseNegInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite(-INFINITY)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void isFiniteMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite(MISSING_NUMERIC)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void isFiniteErrorNoArgument() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite()");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void isFiniteErrorTooManyArguments() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite(23,\"blob\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void isFiniteErrorWrongTypeDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite(date_now())");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void isFiniteErrorWrongTypeNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite(\"Menschenmaterial\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void isFiniteErrorWrongTypeBool() {
		try {
			Expression expression = getExpressionWithFunctionContext("isFinite(TRUE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}
}
