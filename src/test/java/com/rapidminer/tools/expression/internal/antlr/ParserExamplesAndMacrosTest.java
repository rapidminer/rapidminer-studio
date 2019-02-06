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
package com.rapidminer.tools.expression.internal.antlr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.MacroHandler;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.PolynominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.ExpressionParserBuilder;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.MacroResolver;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for example values and macros.
 *
 * @author Gisa Schaefer
 *
 */
public class ParserExamplesAndMacrosTest {

	private static ExampleSet exampleSet;
	private static ExampleResolver resolver;
	private static ExpressionParser parser;

	@BeforeClass
	public static void setUpForAll() {
		exampleSet = makeExampleSet();
		resolver = new ExampleResolver(exampleSet);

		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("my macro", "my value");
		handler.addMacro("Number_macro", "5");
		handler.addMacro("Attribute Macro", "numerical");
		handler.addMacro("my\\ {bracket}", "bracket");
		MacroResolver macroResolver = new MacroResolver(handler);

		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		parser = builder.withDynamics(resolver).withScope(macroResolver).build();
	}

	private static ExampleSet makeExampleSet() {
		List<Attribute> attributes = new LinkedList<>();
		Map<Integer, String> nominalMapping = new HashMap<>();
		nominalMapping.put(0, "cat");
		nominalMapping.put(1, "dog");
		Attribute nominal = AttributeFactory.createAttribute("nominal", Ontology.NOMINAL);
		nominal.setMapping(new PolynominalMapping(nominalMapping));
		attributes.add(nominal);
		attributes.add(AttributeFactory.createAttribute("numerical", Ontology.NUMERICAL));
		attributes.add(AttributeFactory.createAttribute("date_time", Ontology.DATE_TIME));
		attributes.add(AttributeFactory.createAttribute("real]", Ontology.REAL));
		attributes.add(AttributeFactory.createAttribute("integer", Ontology.INTEGER));
		attributes.add(AttributeFactory.createAttribute("date", Ontology.DATE));
		attributes.add(AttributeFactory.createAttribute("t[i]m\\e", Ontology.TIME));

		ExampleSetBuilder builder = ExampleSets.from(attributes);
		double[] data = { 0, 1.5, 123142311234.0, 5, 1123424, 142313411234.0, 1423411234.0 };
		builder.addRow(data);
		data = new double[] { 1, 0.1234123134, 123142313411234.0, 11.25, 11.123, 1423131234.0, 1423434.0 };
		builder.addRow(data);
		data = new double[] { Double.NaN, Double.NaN, Double.NaN, 0.13445e-12, Double.NaN, Double.NaN, Double.NaN };
		builder.addRow(data);

		return builder.build();
	}

	private Expression getExpressionWithExamplesAndMacros(String expression) throws ExpressionException {
		return parser.parse(expression);
	}

	@Test
	public void nominalAttribute() {

		try {
			Expression expression = getExpressionWithExamplesAndMacros("nominal");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());

			for (Example example : exampleSet) {
				resolver.bind(example);
				Attribute attribute = example.getAttributes().get("nominal");
				assertEquals(Double.isNaN(example.getValue(attribute)) ? null : example.getNominalValue(attribute),
						expression.evaluateNominal());
			}

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void numericalAttribute() {

		try {
			Expression expression = getExpressionWithExamplesAndMacros("[numerical]");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());

			for (Example example : exampleSet) {
				resolver.bind(example);
				assertEquals(example.getNumericalValue(example.getAttributes().get("numerical")),
						expression.evaluateNumerical(), 1e-15);
			}

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void realAttribute() {

		try {

			Expression expression = getExpressionWithExamplesAndMacros("[real\\]]");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());

			for (Example example : exampleSet) {
				resolver.bind(example);
				assertEquals(example.getNumericalValue(example.getAttributes().get("real]")),
						expression.evaluateNumerical(), 1e-15);
			}

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void integerAttribute() {

		try {

			Expression expression = getExpressionWithExamplesAndMacros("integer");
			assertEquals(ExpressionType.INTEGER, expression.getExpressionType());

			for (Example example : exampleSet) {
				resolver.bind(example);
				assertEquals(Math.floor(example.getNumericalValue(example.getAttributes().get("integer"))),
						expression.evaluateNumerical(), 1e-15);
			}

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateTimeAttribute() {

		try {
			Expression expression = getExpressionWithExamplesAndMacros("[date_time]");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());

			for (Example example : exampleSet) {
				resolver.bind(example);
				Attribute attribute = example.getAttributes().get("date_time");
				assertEquals(Double.isNaN(example.getValue(attribute)) ? null : example.getDateValue(attribute),
						expression.evaluateDate());
			}

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void dateAttribute() {

		try {
			Expression expression = getExpressionWithExamplesAndMacros("date");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());

			for (Example example : exampleSet) {
				resolver.bind(example);
				Attribute attribute = example.getAttributes().get("date");
				assertEquals(Double.isNaN(example.getValue(attribute)) ? null : example.getDateValue(attribute),
						expression.evaluateDate());
			}

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void timeAttribute() {

		try {
			Expression expression = getExpressionWithExamplesAndMacros("[t\\[i\\]m\\\\e]");
			assertEquals(ExpressionType.DATE, expression.getExpressionType());

			for (Example example : exampleSet) {
				resolver.bind(example);
				Attribute attribute = example.getAttributes().get("t[i]m\\e");
				assertEquals(Double.isNaN(example.getValue(attribute)) ? null : example.getDateValue(attribute),
						expression.evaluateDate());
			}

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void myMacro() {

		try {
			Expression expression = getExpressionWithExamplesAndMacros("%{my macro}");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("my value", expression.evaluateNominal());

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void myBracketMacro() {

		try {
			Expression expression = getExpressionWithExamplesAndMacros("%{my\\\\ \\{bracket\\}}");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("bracket", expression.evaluateNominal());

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void numberMacro() {

		try {
			Expression expression = getExpressionWithExamplesAndMacros("%{Number_macro}");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("5", expression.evaluateNominal());

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void attributeMacro() {

		try {
			Expression expression = getExpressionWithExamplesAndMacros("#{Attribute Macro}");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());

			for (Example example : exampleSet) {
				resolver.bind(example);
				assertEquals(example.getNumericalValue(example.getAttributes().get("numerical")),
						expression.evaluateNumerical(), 1e-15);
			}

		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void unknownMacro() {
		try {
			getExpressionWithExamplesAndMacros("%{unknown}");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void unknownAttribute() {
		try {
			getExpressionWithExamplesAndMacros("[unknown]");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void unknownVariable() {
		try {
			getExpressionWithExamplesAndMacros("unknown");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

}
