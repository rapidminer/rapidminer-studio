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

import java.util.ArrayList;
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
 * Tests the results of {@link AntlrParser#parse(String)} for the attribute eval function.
 *
 * @author Gisa Meier
 *
 */
public class AntlrParserAttributeEvalTest extends AntlrParserTest {

	@Test
	public void evalWrongNumberOfArguments() {
		try {
			getExpressionWithFunctionContext("attribute()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void evalWrongInputType() {
		try {
			getExpressionWithFunctionContext("attribute(2 ,1.5 )");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}


	@Test
	public void withEvalWithMacro() {
		try {
			MacroHandler handler = new MacroHandler(null);
			handler.addMacro("my macro", "\"int\"+\"eger\"");
			MacroResolver macroResolver = new MacroResolver(handler);

			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamplesAndMacros("attribute(eval(%{my macro}))", resolver, macroResolver);
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			resolver.bind(exampleSet.getExample(1));
			assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalWithMacroAndAttribute() {
		try {
			MacroHandler handler = new MacroHandler(null);
			handler.addMacro("my macro", "numeric");
			handler.addMacro("al", "al");
			MacroResolver macroResolver = new MacroResolver(handler);

			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamplesAndMacros(
					"eval(\"attribute(%{my macro}+%{al})*2\")", resolver, macroResolver);
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			resolver.bind(exampleSet.getExample(0));
			assertEquals(2 * 1.5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeConstantNaN() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			getExpressionWithFunctionsAndExamples("attribute(MISSING_NOMINAL)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void evalAttributeConstant() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("attribute(\"nominal 1\")", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			assertEquals("a", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeConstantWrongType() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			getExpressionWithFunctionsAndExamples("attribute(\"nominal1\", REAL)", resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void evalAttributeNonConstant() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("attribute(\"nominal \"+integer, NOMINAL)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			assertEquals("a", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeNonConstantWrongType() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("attribute(\"nominal\"+integer, REAL)", resolver);
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			expression.evaluateNumerical();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void evalAttributeNonConstantNaN() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("attribute(\"nominal\"+integer, NOMINAL)", resolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(1));
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	private ExampleSet makeExampleSet() {
		List<Attribute> attributes = new ArrayList<>();
		Map<Integer, String> nominalMapping = new HashMap<>();
		nominalMapping.put(0, "a");
		nominalMapping.put(1, "b");
		Attribute nominal = AttributeFactory.createAttribute("nominal 1", Ontology.NOMINAL);
		nominal.setMapping(new PolynominalMapping(nominalMapping));
		attributes.add(nominal);
		attributes.add(AttributeFactory.createAttribute("numerical", Ontology.NUMERICAL));
		attributes.add(AttributeFactory.createAttribute("integer", Ontology.INTEGER));

		ExampleSetBuilder builder = ExampleSets.from(attributes);
		double[] data = { 0, 1.5, 1 };
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
			Expression expression = getExpressionWithFunctionsAndExamples("attribute(\"int\"+ \"eger\",INTEGER)", resolver);
			resolver.bind(exampleSet.getExample(0));

			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
			assertEquals(1, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void evalAttributeWithSecondToDouble() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("attribute(lower(\"NUmerical\"),REAL)", resolver);
			resolver.bind(exampleSet.getExample(0));

			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(1.5, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}


	@Test
	public void evalAttributeWithSecondNotConstant() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			getExpressionWithFunctionsAndExamples("attribute(\"integer\",[nominal])", resolver);
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
			getExpressionWithFunctionsAndExamples("attribute(\"nominal\", MISSING_NOMINAL)",
					resolver);
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}


	@Test
	public void evalAttributeWithSecondToDate() {
		try {
			ExampleSet exampleSet = makeExampleSet();
			ExampleResolver resolver = new ExampleResolver(exampleSet);
			Expression expression = getExpressionWithFunctionsAndExamples("attribute(\"inte\"+\"ger\",DATE)", resolver);
			assertEquals(ExpressionType.DATE, expression.getExpressionType());
			resolver.bind(exampleSet.getExample(0));
			expression.evaluateDate();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
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
			Expression expression = getExpressionWithFunctionsAndExamplesAndMacros("attribute(%{attribute})", resolver,
					macroResolver);
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			assertEquals(1, expression.evaluateNumerical(), 1e-15);
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
			Expression expression = getExpressionWithFunctionsAndExamplesAndMacros("attribute(%{attribute},NOMINAL)", resolver,
					macroResolver);
			assertEquals(ExpressionType.STRING, expression.getExpressionType());

			resolver.bind(exampleSet.getExample(0));
			assertEquals("1", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

}
