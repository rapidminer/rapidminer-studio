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

import java.util.Date;
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
import com.rapidminer.tools.expression.internal.function.logical.If;


/**
 * JUnit test for the {@link If}, Function of the antlr ExpressionParser
 *
 * @author Sabrina Kirstein
 *
 */
public class AnltrParserIfTest extends AntlrParserTest {

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

	@Test
	public void ifConstantConditionBooleanTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(TRUE,TRUE,FALSE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionBooleanFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(FALSE,TRUE,FALSE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionNumericTrue() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(1,TRUE,FALSE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionNumericFalse() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(0,TRUE,FALSE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(\"test\",TRUE,FALSE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionDate() {
		try {
			ExampleSet exampleSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("if([TestDate],TRUE,FALSE)", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			expression.evaluateBoolean();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionBooleanMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(contains(MISSING_NOMINAL,\"test\"),TRUE,FALSE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionNumericMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(MISSING_NUMERIC,TRUE,FALSE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionTrueIfBlockBoolean() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(TRUE,FALSE,4)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionTrueIfBlockBooleanMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(TRUE,contains(MISSING_NOMINAL,\"test\"),4)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionTrueIfBlockInteger() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(TRUE,4,FALSE)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(4, expression.evaluateNumerical(), 0);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionTrueIfBlockDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(3==3,4.5,FALSE)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(4.5, expression.evaluateNumerical(), 0);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionTrueIfBlockDoubleMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(3==3,MISSING_NUMERIC,FALSE)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 0);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionTrueIfBlockDate() {
		try {
			ExampleSet exampleSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("if(TRUE,[TestDate],FALSE)", resolver);
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(expression.evaluateDate(), new Date(sometime));
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionTrueIfBlockDateMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(TRUE,MISSING_DATE,FALSE)");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionTrueIfBlockNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(3==3,\"test\",FALSE)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("test", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionTrueIfBlockNominalMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(3==3,MISSING_NOMINAL,FALSE)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionFalseElseBlockBoolean() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(FALSE,4,TRUE)");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertTrue(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionFalseElseBlockBooleanMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(FALSE,4,contains(MISSING_NOMINAL,\"test\"))");
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertEquals(null, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionFalseElseBlockInteger() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(FALSE,TRUE,4)");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(4, expression.evaluateNumerical(), 0);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionFalseElseBlockDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(3!=3,FALSE,4.5)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(4.5, expression.evaluateNumerical(), 0);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionFalseElseBlockDoubleMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(3!=3,FALSE,MISSING_NUMERIC)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(Double.NaN, expression.evaluateNumerical(), 0);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionFalseElseBlockDate() {
		try {
			ExampleSet exampleSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			resolver.bind(exampleSet.getExample(0));
			// use two attributes (date_time type) to compare dates
			Expression expression = getExpressionWithFunctionsAndExamples("if(FALSE,TRUE,[TestDate])", resolver);
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(expression.evaluateDate(), new Date(sometime));
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionFalseElseBlockDateMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(FALSE,TRUE,MISSING_DATE)");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(null, expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionFalseElseBlockNominal() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(3!=3,FALSE,\"test\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("test", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifConstantConditionFalseElseBlockNominalMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("if(3!=3,FALSE,MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	// check return type calculation for dynamic condition

	@Test
	public void ifDynamicConditionIfBlockEqualTypeElseBlock() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if([Test],[TestDate],MISSING_DATE)", resolver);
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			assertEquals(new Date(sometime), expression.evaluateDate());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockIntElseBlockDouble() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if([Test],1,3.4)", resolver);
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1, expression.evaluateNumerical(), 0);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockBoolean() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],TRUE,FALSE)", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
			assertFalse(expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockString() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if([Test],TRUE,\"TEST\")", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("true", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockInteger() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],TRUE,3)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("3", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockDouble() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],TRUE,3.5)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("3.5", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockDoubleInfinity() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],TRUE,INFINITY)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("\u221E", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockDoubleMinusInfinity() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],TRUE,-INFINITY)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("-\u221E", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockDate() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],TRUE,[TestDate])", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			Date testDate = new Date(sometime);
			assertEquals(testDate.toString(), expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockBoolean() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if([Test],\"TEST\",TRUE)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("TEST", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockMissingBoolean() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples(
					"if(![Test],\"TEST\",contains(MISSING_NOMINAL,MISSING_NOMINAL))", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockMissingDate() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],\"TEST\",MISSING_DATE)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockMissingNumeric() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],\"TEST\",MISSING_NUMERIC)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockMissingNominal() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],\"TEST\",MISSING_NOMINAL)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockMissingBooleanElseBlockString() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples(
					"if([Test],contains(MISSING_NOMINAL,MISSING_NOMINAL),\"TEST\")", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockMissingDateElseBlockString() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if([Test],MISSING_DATE,\"TEST\")", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockMissingNumericElseBlockString() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if([Test],MISSING_NUMERIC,\"TEST\")", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void ifDynamicConditionIfBlockMissingNominalElseBlockString() {
		try {
			ExampleSet testSet = makeTestExampleSet();
			ExampleResolver resolver = new ExampleResolver(testSet);
			resolver.bind(testSet.getExample(0));
			// use an attribute to ensure a dynamic condition
			Expression expression = getExpressionWithFunctionsAndExamples("if(![Test],\"TEST\",MISSING_NOMINAL)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

}
