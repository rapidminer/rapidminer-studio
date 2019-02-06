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
package com.rapidminer.tools.expression.internal.function.logical;

import java.util.concurrent.Callable;

import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;


/**
 * Abstract class for a function that has 1 logical (numerical, true or false) input
 *
 * @author Sabrina Kirstein
 *
 */
public abstract class AbstractLogicalFunctionWith1Input extends AbstractLogicalFunction {

	/**
	 * Constructs a logical AbstractFunction with 1 parameter with {@link FunctionDescription}
	 * generated from the arguments and the function name generated from the description.
	 *
	 * @param i18nKey
	 *            the key for the {@link FunctionDescription}. The functionName is read from
	 *            "gui.dialog.function.i18nKey.name", the helpTextName from ".help", the groupName
	 *            from ".group", the description from ".description" and the function with
	 *            parameters from ".parameters". If ".parameters" is not present, the ".name" is
	 *            taken for the function with parameters.
	 */
	public AbstractLogicalFunctionWith1Input(String i18nKey) {
		super(i18nKey, 1);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {

		if (inputEvaluators.length != 1) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 1,
					inputEvaluators.length);
		}
		ExpressionType type = getResultType(inputEvaluators);
		ExpressionEvaluator evaluator = inputEvaluators[0];

		return new SimpleExpressionEvaluator(makeBooleanCallable(evaluator), isResultConstant(inputEvaluators), type);
	}

	/**
	 * Builds a boolean callable from evaluator using {@link #compute(double)} or
	 * {@link #compute(boolean)}, where constant child results are evaluated.
	 *
	 * @param evaluator
	 * @return the resulting boolean callable
	 */
	protected Callable<Boolean> makeBooleanCallable(ExpressionEvaluator evaluator) {
		ExpressionType inputType = evaluator.getType();

		if (inputType.equals(ExpressionType.DOUBLE) || inputType.equals(ExpressionType.INTEGER)) {

			final DoubleCallable func = evaluator.getDoubleFunction();
			try {
				if (evaluator.isConstant()) {

					final Boolean result = compute(func.call());
					return new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return result;
						}
					};
				} else {
					return new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return compute(func.call());
						}
					};
				}
			} catch (ExpressionParsingException e) {
				throw e;
			} catch (Exception e) {
				throw new ExpressionParsingException(e);
			}
		} else if (inputType.equals(ExpressionType.BOOLEAN)) {

			final Callable<Boolean> func = evaluator.getBooleanFunction();
			try {
				if (evaluator.isConstant()) {

					final Boolean result = compute(func.call());
					return new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return result;
						}
					};
				} else {
					return new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return compute(func.call());
						}
					};
				}
			} catch (ExpressionParsingException e) {
				throw e;
			} catch (Exception e) {
				throw new ExpressionParsingException(e);
			}
		} else {
			return null;
		}
	}

	/**
	 * Computes the result for a double value.
	 *
	 * @param value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(double value);

	/**
	 * Computes the result for a boolean value.
	 *
	 * @param value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(Boolean value);
}
