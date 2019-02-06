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

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.ExpressionParserBuilder;
import com.rapidminer.tools.expression.ExpressionRegistry;
import com.rapidminer.tools.expression.MacroResolver;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)}.
 *
 * The tests for each function should include at least:
 * <ul>
 * <li>a single correct value</li>
 * <li>two correct values</li>
 * <li>an incorrect value</li>
 * <li>combinations of correct and incorrect types</li>
 * <li>other special cases unique to the function or group of function</li>
 * </ul>
 *
 *
 * @author Gisa Schaefer
 *
 */
public class AntlrParserTest {

	/**
	 * Parses string expressions into {@link Expression}s if those expressions don't use any
	 * functions, macros, attributes or other variables.
	 */
	protected Expression getExpressionWithoutContext(String expression) throws ExpressionException {
		AntlrParser parser = new AntlrParser(null);
		return parser.parse(expression);
	}

	/**
	 * Parses string expressions into {@link Expression}s if those expressions use known functions
	 * and no macros, attributes or other variables.
	 */
	protected Expression getExpressionWithFunctionContext(String expression) throws ExpressionException {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		ExpressionParser parser = builder.withModules(ExpressionRegistry.INSTANCE.getAll()).build();
		return parser.parse(expression);
	}

	/**
	 * Parses string expressions into {@link Expression}s if those expressions use known functions
	 * and macros but no attributes or other variables.
	 */
	protected Expression getExpressionWithFunctionsAndMacros(String expression, MacroResolver resolver)
			throws ExpressionException {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		ExpressionParser parser = builder.withModules(ExpressionRegistry.INSTANCE.getAll()).withScope(resolver).build();
		return parser.parse(expression);
	}

	/**
	 * Parses the string expression into a {@link Expression} using the given
	 * {@link ExampleResolver}.
	 */
	protected Expression getExpressionWithFunctionsAndExamples(String expression, ExampleResolver resolver)
			throws ExpressionException {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		ExpressionParser parser = builder.withModules(ExpressionRegistry.INSTANCE.getAll()).withDynamics(resolver).build();
		return parser.parse(expression);
	}

	/**
	 * Parses the string expression into a {@link Expression} using the given
	 * {@link ExampleResolver} and the given {@link MacroResolver}.
	 *
	 * @throws ExpressionException
	 */
	protected Expression getExpressionWithFunctionsAndExamplesAndMacros(String expression, ExampleResolver resolver,
			MacroResolver macroResolver) throws ExpressionException {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		ExpressionParser parser = builder.withModules(ExpressionRegistry.INSTANCE.getAll()).withDynamics(resolver)
				.withScope(macroResolver).build();
		return parser.parse(expression);
	}

	/**
	 * @return a exampleSet containing a single example with a single integer attribute called
	 *         "integer" with value NaN.
	 */
	protected ExampleSet makeMissingIntegerExampleSet() {
		List<Attribute> attributes = new LinkedList<>();
		attributes.add(AttributeFactory.createAttribute("integer", Ontology.INTEGER));

		ExampleSetBuilder builder = ExampleSets.from(attributes);
		double[] data = new double[] { Double.NaN };
		builder.addRow(data);

		return builder.build();
	}

}
