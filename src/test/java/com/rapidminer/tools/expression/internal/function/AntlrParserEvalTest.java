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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.rapidminer.MacroHandler;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.PolynominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.MacroResolver;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for the eval function.
 *
 * @author Gisa Schaefer
 *
 */
public class AntlrParserEvalTest extends AntlrParserTest {

	@Test
	public void evalWrongNumberOfArguments() {
		try {
			getExpressionWithFunctionContext("eval()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void evalWrongInputType() {
		try {
			getExpressionWithFunctionContext("eval(2 ,1.5 )");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void multiplyIntsViaEval() {
		try {
			Expression expression = getExpressionWithFunctionContext("eval(\"3*5\")");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(3 * 5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalMissingString() {
		try {
			Expression expression = getExpressionWithFunctionContext("eval(MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void multiplyIntsViaEvalToDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("eval(\"3*5\",REAL)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(3 * 5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void multiplyDoublesViaEval() {
		try {
			Expression expression = getExpressionWithFunctionContext("eval(\"3.0\"+\"*5\")");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(3.0 * 5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void divideIntsViaEvalWithType() {
		try {
			Expression expression = getExpressionWithFunctionContext("eval(\"4 /2\",REAL)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(4.0 / 2, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void divideDoublesViaEvalWithWrongType() {
		try {
			getExpressionWithFunctionContext("eval(\"5.0 /2\",INTEGER)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void evalWithInvalidType() {
		try {
			getExpressionWithFunctionContext("eval(\"5.0 /2\",\"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void moduloIntsViaEvalWithStringType() {
		try {
			Expression expression = getExpressionWithFunctionContext("eval(\"5 %2\",NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(5 % 2 + "", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalWithStringTypeMissing() {
		try {
			Expression expression = getExpressionWithFunctionContext("eval(\"MISSING_NOMINAL\",NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalWithStringTypeMissingNumerical() {
		try {
			Expression expression = getExpressionWithFunctionContext("eval(\"MISSING_NUMERIC\",NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalWithStringTypeMissingDate() {
		try {
			Expression expression = getExpressionWithFunctionContext("eval(\"MISSING_DATE\",NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void differentPointOperationsWithNestedEvalFail() {
		try {
			getExpressionWithFunctionContext("eval(\"4%3 *\"+\"eval(\"+\"5/2\"+\")\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void powerIntsEvalWithMacro() {
		try {
			MacroHandler handler = new MacroHandler(null);
			handler.addMacro("my macro", "2^3^2");
			MacroResolver resolver = new MacroResolver(handler);

			Expression expression = getExpressionWithFunctionsAndMacros("eval(%{my macro})", resolver);
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(Math.pow(2, Math.pow(3, 2)), expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void differentPointOperationsWithNestedEval() {
		try {
			MacroHandler handler = new MacroHandler(null);
			handler.addMacro("my macro", "\"5/2\"");
			MacroResolver resolver = new MacroResolver(handler);

			Expression expression = getExpressionWithFunctionsAndMacros("eval(\"4%3 *\"+\"eval(\"+%{my macro}+\")\")",
					resolver);
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(4 % 3 * 5 / 2.0, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeWithoutSecond() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			getExpressionWithFunctionsAndExamples("eval([nominal])", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	private ExampleSet makeExampleSet() {
		List<Attribute> attributes = new LinkedList<>();
		Map<Integer, String> nominalMapping = new HashMap<>();
		nominalMapping.put(0, "contains(\"a\", \"b\")");
		nominalMapping.put(1, "concat(\"a\", \"b\")");
		Attribute nominal = AttributeFactory.createAttribute("nominal", Ontology.NOMINAL);
		nominal.setMapping(new PolynominalMapping(nominalMapping));
		attributes.add(nominal);
		attributes.add(AttributeFactory.createAttribute("numerical", Ontology.NUMERICAL));
		attributes.add(AttributeFactory.createAttribute("integer", Ontology.INTEGER));

		ExampleSetBuilder builder = ExampleSets.from(attributes);
		double[] data = { 0, 1.5, 5 };
		builder.addRow(data);
		data = new double[] { 1, 3.0, Double.NaN };
		builder.addRow(data);

		return builder.build();
	}

	@Test
	public void evalAttributeWithSecond() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval(\"3*\"+[integer],INTEGER)", resolver);
			resolver.bind(exampleSet.getExample(0));

			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(3 * 5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeWithSecondToDouble() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval(\"3*\"+[integer],REAL)", resolver);
			resolver.bind(exampleSet.getExample(0));

			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(3.0 * 5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeWithSecondToString() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval(\"3*\"+[integer],NOMINAL)", resolver);
			resolver.bind(exampleSet.getExample(0));

			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(3 * 5 + "", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeWithSecondNotConstant() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			getExpressionWithFunctionsAndExamples("eval(\"3*\"+[integer],[nominal])", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void evalAttributeWithSecondToStringMissing() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval(\"MISSING_DATE\"+[integer],NOMINAL)",
					resolver);
			resolver.bind(exampleSet.getExample(1));

			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeWithSecondToStringMissingBoth() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval(MISSING+[integer],NOMINAL)", resolver);
			resolver.bind(exampleSet.getExample(1));

			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeWithSecondToDate() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval(\"3*\"+[integer],DATE)", resolver);
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			resolver.bind(exampleSet.getExample(0));
			expression.evaluateDate();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void evalRealAttributeWithSecondToDouble() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval(\"3*\"+[numerical],REAL)", resolver);
			resolver.bind(exampleSet.getExample(0));

			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(3 * 1.5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalRealAttributeWithSecondToInteger() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval(\"3*\"+[numerical],INTEGER)", resolver);
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			resolver.bind(exampleSet.getExample(0));
			expression.evaluateNumerical();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void evalStringAttributeWithSecond() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval([nominal],NOMINAL)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			assertEquals("false", expression.evaluateNominal());

			resolver.bind(exampleSet.getExample(1));
			assertEquals("ab", expression.evaluateNominal());

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalBooleanAttributeWithSecond() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval([nominal],BINOMINAL)", resolver);
			assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			assertEquals(false, expression.evaluateBoolean());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeFromString() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("eval(\"integer\")", resolver);
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			assertEquals(5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void attributeFromMacro() {
		try {
			MacroHandler handler = new MacroHandler(null);
			handler.addMacro("attribute", "integer");
			MacroResolver macroResolver = new MacroResolver(handler);

			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamplesAndMacros("eval(%{attribute})", resolver,
					macroResolver);
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			assertEquals(5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void attributeFromMacroWithType() {
		try {
			MacroHandler handler = new MacroHandler(null);
			handler.addMacro("attribute", "integer");
			MacroResolver macroResolver = new MacroResolver(handler);

			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamplesAndMacros("eval(%{attribute},NOMINAL)", resolver,
					macroResolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			assertEquals("5", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

}
