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

import java.util.Calendar;
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
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for date functions.
 *
 * @author David Arnu
 *
 */
public class AntlrParserDateTest extends AntlrParserTest {

	// double values for some date entries
	static double sometime = 123142313411234.0;
	static double sometime_before = sometime - 5000;
	static double sometime_after = sometime + 5000;

	private static ExampleSet makeDateExampleSet() {
		List<Attribute> attributes = new LinkedList<>();

		attributes.add(AttributeFactory.createAttribute("date_time", Ontology.DATE_TIME));
		attributes.add(AttributeFactory.createAttribute("date_time_before", Ontology.DATE_TIME));
		attributes.add(AttributeFactory.createAttribute("date_time_after", Ontology.DATE_TIME));
		attributes.add(AttributeFactory.createAttribute("date_time_missing", Ontology.DATE_TIME));
		attributes.add(AttributeFactory.createAttribute("integer", Ontology.INTEGER));

		ExampleSetBuilder builder = ExampleSets.from(attributes);
		double[] data = { sometime, sometime_before, sometime_after, Double.NaN, Double.NaN };
		builder.addRow(data);
		builder.addRow(data);
		builder.addRow(data);
		builder.addRow(data);

		return builder.build();
	}

	// date_now
	@Test
	public void dateNowBasic() {
		try {

			Expression expression = getExpressionWithFunctionContext("date_now()");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			// for testing we just assume that the dates are close enough, some delay might occur
			assertTrue("Dates aren't close enough to each other!",
					Math.abs(Calendar.getInstance().getTime().getTime() - expression.evaluateDate().getTime()) < 10);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateNowArgumentNumerical() {
		try {
			getExpressionWithFunctionContext("date_now(5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateNowArgumentString() {
		try {
			getExpressionWithFunctionContext("date_now(\"bla\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// date_before

	@Test
	public void dateBeforeTRUE() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_before(date_time, date_time_after)",
					resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateBeforeFALSE() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_before(date_time, date_time_before)",
					resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateBeforeEqual() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_after(date_time, date_time)", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateBeforeMissingFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_before(date_time_missing, date_time)",
					resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateBeforeMissingSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_before(date_time, date_time_missing)",
					resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateBeforeWrongFirst() {
		try {
			getExpressionWithFunctionContext("date_before(\"bla\", date_now())");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateBeforeWrongSecond() {
		try {
			getExpressionWithFunctionContext("date_before(date_now(), \"bla\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// date_after

	@Test
	public void dateAfterTRUE() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_after(date_time, date_time_before)",
					resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAfterFALSE() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_after(date_time, date_time_after)", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAfterEqual() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_after(date_time, date_time)", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAfterMissingFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_after(date_time_missing, date_time)",
					resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAfterMissingSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_after(date_time, date_time_missing)",
					resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAfterWrongFirst() {
		try {
			getExpressionWithFunctionContext("date_after(\"bla\", date_now())");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateAfterWrongSecond() {
		try {
			getExpressionWithFunctionContext("date_after(date_now(), \"bla\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// date_diff
	@Test
	public void dateDiffBasic() {
		try {
			Expression expression = getExpressionWithFunctionContext("date_diff(date_now(), date_now())");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			// for testing we just assume that the dates are close enough, some delay might occur
			assertTrue("Dates aren't close enough to each other!", Math.abs(expression.evaluateNumerical()) < 10);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateDiffPositive() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_diff(date_time, date_time_after)", resolver);
			assertEquals(sometime_after - sometime, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateDiffNegative() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_diff(date_time, date_time_before)", resolver);
			assertEquals(sometime_before - sometime, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateDiffEqual() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_diff(date_time, date_time)", resolver);
			assertEquals(0, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateDiffMissingFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_diff(date_time_missing, date_time)",
					resolver);
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateDiffMissingSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_diff(date_time, date_time_missing)",
					resolver);
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateDiffWrongFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionContext("date_diff(\"bla\", date_now())");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateDiffWrongSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionContext("date_diff(date_now(), \"bla\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateDiffWithLocaleTZ() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples(
					"date_diff(date_time, date_time_after, \"us\", \"America/Los Angeles\")", resolver);
			assertEquals(sometime_after - sometime, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateDiffLocaleTZWrongFormat() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples(
					"date_diff(date_time, date_time_after, \"abcd\", \"abcd\")", resolver);
			assertEquals(sometime_after - sometime, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateDiffLocaleTZWrongType() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_diff(date_time, date_time_after, 5, 5)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateDiffLocaleTZMissing() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples(
					"date_diff(date_time, date_time_after, MISSING_NOMINAL, MISSING_NOMINAL)", resolver);
			assertEquals(sometime_after - sometime, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	// date_add

	@Test
	public void dateAddBasic() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_add(date_now(), 5, DATE_UNIT_HOUR)",
					resolver);
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR, 5);
			double diff = cal.getTime().getTime() - expression.evaluateDate().getTime();
			assertTrue("Dates aren't close enough to each other!", Math.abs(diff) < 10);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	public void dateAddBasicWithLocaleTZ() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples(
					"date_add(date_now(), 5, DATE_UNIT_HOUR, \"us\", \"America/Los Angeles\")", resolver);
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR, 5);
			double diff = cal.getTime().getTime() - expression.evaluateDate().getTime();
			assertTrue("Dates aren't close enough to each other!", Math.abs(diff) < 10);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	public void dateAddBasicWithLocaleTZMissing() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples(
					"date_add(date_now(), 5, DATE_UNIT_HOUR, MISSNING_NOMINAL, MISSNING_NOMINAL)", resolver);
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR, 5);
			double diff = cal.getTime().getTime() - expression.evaluateDate().getTime();
			assertTrue("Dates aren't close enough to each other!", Math.abs(diff) < 10);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAddMissingFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_add(date_time_missing, 5, DATE_UNIT_HOUR)",
					resolver);
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAddMissingSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_add(date_time, [integer], DATE_UNIT_HOUR)",
					resolver);
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAddMissingThird() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_add(date_time, 5, MISSING_NOMINAL)",
					resolver);
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAddWrongFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_add(5, 5, DATE_UNIT_HOUR)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateAddWrongSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_add(date_time, \"abc\", DATE_UNIT_HOUR)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateAddWrongThirdType() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_add(date_time, 5, 5)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateAddWrongThirdConstant() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_add(date_time, 5, \"abc\")", resolver);
			// fails during runtime
			expression.evaluateDate();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateAddWrongLocaleTZ() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_add(date_time, 5, DATE_UNIT_HOUR , 5,,5)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// date_set

	@Test
	public void dateSetBasic() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_set(date_time, 11, DATE_UNIT_MONTH)",
					resolver);
			assertEquals(11, expression.evaluateDate().getMonth());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateSetBasicWithLocaleTZ() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples(
					"date_set(date_time, 10, DATE_UNIT_MINUTE, \"us\", \"America/Los Angeles\")", resolver);
			assertEquals(10, expression.evaluateDate().getMinutes());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateSetBasicWithLocaleTZMissing() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples(
					"date_set(date_now(), 5, DATE_UNIT_HOUR, MISSING_NOMINAL, MISSING_NOMINAL)", resolver);
			assertEquals(5, expression.evaluateDate().getHours());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateSetMissingFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_set(date_time_missing, 5, DATE_UNIT_HOUR)",
					resolver);
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateSetMissingSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_set(date_time, [integer], DATE_UNIT_HOUR)",
					resolver);
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateSetMissingThird() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_set(date_time, 5, MISSING_NOMINAL)",
					resolver);
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateSetWrongFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_set(5, 5, DATE_UNIT_HOUR)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateSetWrongSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_set(date_time, \"abc\", DATE_UNIT_HOUR)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateSetWrongThirdType() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_set(date_time, 5, 5)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateSetWrongThirdConstant() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_set(date_time, 5, \"abc\")", resolver);
			// fails during runtime
			expression.evaluateDate();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateSetWrongLocaleTZ() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_set(date_time, 5, DATE_UNIT_HOUR , 5, 5)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// date_get

	@Test
	public void dateGetBasic() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_get(date_time, DATE_UNIT_HOUR)", resolver);
			assertEquals(7, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateGetMissingFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_get(date_time_missing, DATE_UNIT_HOUR)",
					resolver);
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateGetMissingSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_get(date_time, MISSING_NOMINAL)", resolver);
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateGetBasicWithLocaleTZ() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_get(date_time, DATE_UNIT_HOUR)", resolver);
			assertEquals(7, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateGetWrongFirst() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_get(5, DATE_UNIT_HOUR, \"us\", \"America/Los Angeles\")", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateGetWrongSecond() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_get(date_time, 5.5 )", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateGetWrongUnit() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_get(date_time, \"X\")", resolver);
			expression.evaluate();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateGetMissingLocaleTZ() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples(
					"date_get(date_time, DATE_UNIT_HOUR, MISSING_NOMINAL, MISSING_NOMINAL)", resolver);
			assertEquals(7, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateGetWrongLocaleTZ() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			getExpressionWithFunctionsAndExamples("date_get(date_time, DATE_UNIT_HOUR, 5, 5)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// date_time
	@Test
	public void dateMillis() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_millis(date_time)", resolver);
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(sometime, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateMillisWrongType() {
		try {
			getExpressionWithFunctionContext("date_millis(5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void dateMillisMissing() {
		try {
			ExampleSet exampleSet = makeDateExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			Expression expression = getExpressionWithFunctionsAndExamples("date_millis(date_time_missing)", resolver);
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

}
