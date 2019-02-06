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
import com.rapidminer.tools.expression.internal.function.logical.And;
import com.rapidminer.tools.expression.internal.function.logical.Not;
import com.rapidminer.tools.expression.internal.function.logical.Or;


/**
 * JUnit test for the Logical Functions ({@link Or}, {@link And} and {@link Not}) of the antlr
 * ExpressionParser
 *
 * @author Thilo Kamradt, Sabrina Kirstein
 *
 */
public class AntlrParserLogicalTest extends AntlrParserTest {

	// long value for some date entry
	static long sometime = 1436792411000l;

	private static ExampleSet makeTestExampleSet() {
		List<Attribute> attributes = new LinkedList<>();

		attributes.add(AttributeFactory.createAttribute("Test", Ontology.INTEGER));
		attributes.add(AttributeFactory.createAttribute("TestDate", Ontology.DATE_TIME));

		ExampleSetBuilder builder = ExampleSets.from(attributes);
		double[] data = { 1, sometime };
		builder.addRow(data);

		return builder.build();
	}

	// and

	@Test
	public void andBooleanFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE && FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void andBooleanTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE && TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void andNumericFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("1 && 0");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void andNumericTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("45.654321 && -45");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void andMixedFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("5456 && FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void andMixedTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("5456 && TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void andBooleanMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("contains(MISSING_NOMINAL,\"Luke\") && FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void andDoubleMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("MISSING_NUMERIC && FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void andWrongTypeNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE && \"baboom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void andWrongTypeDate() {
		try {
			ExampleSet exampleSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("TRUE && [TestDate]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// or

	@Test
	public void orBooleanFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("FALSE || FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void orBooleanTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("FALSE || TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void orNumericFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("0 || 0");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void orNumericTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("0 || 45");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void orMixedFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("0 || FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void orMixedTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("5456 || TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void orBooleanMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("contains(MISSING_NOMINAL,\"Luke\") || FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void orDoubleMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("MISSING_NUMERIC || TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void orWrongTypeNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("TRUE || \"baboom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void orWrongTypeDate() {
		try {
			ExampleSet exampleSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("TRUE || [TestDate]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// not

	@Test
	public void notBooleanTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("!FALSE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notBooleanFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("!TRUE");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notNumericTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("!0");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notNumericFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("!45");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notBooleanMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("!contains(MISSING_NOMINAL,\"Luke\")");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notNumericMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("!MISSING_NUMERIC");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void notWrongTypeNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("!\"baboom\"");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void notWrongTypeDate() {
		try {
			ExampleSet exampleSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("![TestDate]", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}
}
