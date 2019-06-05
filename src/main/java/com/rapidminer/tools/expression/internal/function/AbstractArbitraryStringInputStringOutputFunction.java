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

import java.util.concurrent.Callable;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;


/**
 *
 * Abstract class for a {@link Function} that has arbitrary many String arguments and returns a
 * String argument.
 *
 * @author David Arnu
 *
 */
public abstract class AbstractArbitraryStringInputStringOutputFunction extends AbstractFunction {

	/**
	 * Constructs an AbstractFunction with {@link FunctionDescription} generated from the arguments
	 * and the function name generated from the description.
	 *
	 * @param i18nKey
	 *            the key for the {@link FunctionDescription}. The functionName is read from
	 *            "gui.dialog.function.i18nKey.name", the helpTextName from ".help", the groupName
	 *            from ".group", the description from ".description" and the function with
	 *            parameters from ".parameters". If ".parameters" is not present, the ".name" is
	 *            taken for the function with parameters.
	 * @param numberOfArgumentsToCheck
	 *            the fixed number of parameters this functions expects or -1
	 */
	public AbstractArbitraryStringInputStringOutputFunction(String i18n, int numberOfArgumentsToCheck) {
		super(i18n, numberOfArgumentsToCheck, Ontology.NOMINAL);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		checkNumberOfInputs(inputTypes.length);
		// check if all input arguments are strings
		for (ExpressionType input : inputTypes) {
			if (input != ExpressionType.STRING) {
				throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "nominal");
			}
		}
		return ExpressionType.STRING;
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {

		ExpressionType type = getResultType(inputEvaluators);

		return new SimpleExpressionEvaluator(makeStringCallable(inputEvaluators), type, isResultConstant(inputEvaluators));

	}

	/**
	 * Builds a String callable from an arbitrary number of String input arguments
	 *
	 * @param inputEvaluators
	 *            the input
	 * @return the resulting callable<String>
	 */
	protected Callable<String> makeStringCallable(final ExpressionEvaluator[] inputEvaluators) {
		final int inputLength = inputEvaluators.length;
		try {
			final String[] constantValues = new String[inputLength];
			int i = 0;
			// evaluate which expressions are constant
			for (ExpressionEvaluator exp : inputEvaluators) {
				constantValues[i] = exp.isConstant() ? exp.getStringFunction().call() : "";
				i++;
			}
			// because we can assume that the function is constant, this is a way to check if all
			// values are constant
			if (isResultConstant(inputEvaluators)) {
				// compute the result only once and re-use this
				final String result = compute(constantValues);
				return () -> result;

			}
			return () -> {
				final String[] values = new String[inputLength];
				// collect the constant values and fetch the not constant values
				for (int j = 0; j < inputLength; j++) {
					values[j] = inputEvaluators[j].isConstant() ? constantValues[j] : inputEvaluators[j]
							.getStringFunction().call();
				}
				return compute(values);
			};
		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}

	}

	/**
	 * Computes the result for arbitrary many String values.
	 *
	 * @param String
	 *            values
	 * @return the result of the computation.
	 */
	protected abstract String compute(String... values);

	/**
	 * Checks the number of input values. By default this does nothing but for functions with a
	 * fixed number of arguments a check can be added.
	 *
	 * @param values
	 *            input value
	 */
	protected abstract void checkNumberOfInputs(int length);

}
