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
 * Abstract class for a function that has 2 logical (numerical, true or false) inputs
 *
 * @author Sabrina Kirstein
 *
 */
public abstract class AbstractLogicalFunctionWith2Inputs extends AbstractLogicalFunction {

	/**
	 * Constructs a logical AbstractFunction with 2 parameters with {@link FunctionDescription}
	 * generated from the arguments and the function name generated from the description.
	 *
	 * @param i18nKey
	 *            the key for the {@link FunctionDescription}. The functionName is read from
	 *            "gui.dialog.function.i18nKey.name", the helpTextName from ".help", the groupName
	 *            from ".group", the description from ".description" and the function with
	 *            parameters from ".parameters". If ".parameters" is not present, the ".name" is
	 *            taken for the function with parameters.
	 */
	public AbstractLogicalFunctionWith2Inputs(String i18nKey) {
		super(i18nKey, 2);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {

		if (inputEvaluators.length != 2) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 2,
					inputEvaluators.length);
		}
		ExpressionType type = getResultType(inputEvaluators);

		ExpressionEvaluator left = inputEvaluators[0];
		ExpressionEvaluator right = inputEvaluators[1];

		return new SimpleExpressionEvaluator(makeBooleanCallable(left, right), isResultConstant(inputEvaluators), type);
	}

	/**
	 * Builds a boolean callable from evaluator using {@link #compute(double} or {@link
	 * #compute(boolean}, where constant child results are evaluated.
	 *
	 * @param left
	 *            evaluator
	 * @param right
	 *            evaluator
	 * @return the resulting boolean callable
	 */
	protected Callable<Boolean> makeBooleanCallable(ExpressionEvaluator left, ExpressionEvaluator right) {
		ExpressionType leftType = left.getType();
		ExpressionType rightType = right.getType();

		try {
			if (leftType.equals(ExpressionType.DOUBLE) || leftType.equals(ExpressionType.INTEGER)) {

				final DoubleCallable funcLeft = left.getDoubleFunction();
				final double valueLeft = left.isConstant() ? funcLeft.call() : Double.NaN;

				if (rightType.equals(ExpressionType.DOUBLE) || rightType.equals(ExpressionType.INTEGER)) {

					final DoubleCallable funcRight = right.getDoubleFunction();
					final double valueRight = right.isConstant() ? funcRight.call() : Double.NaN;

					if (left.isConstant() && right.isConstant()) {
						final Boolean result = compute(valueLeft, valueRight);
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return result;
							}
						};
					} else if (left.isConstant()) {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(valueLeft, funcRight.call());
							}
						};
					} else if (right.isConstant()) {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(funcLeft.call(), valueRight);
							}
						};
					} else {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(funcLeft.call(), funcRight.call());
							}
						};
					}

				} else if (rightType.equals(ExpressionType.BOOLEAN)) {

					final Callable<Boolean> funcRight = right.getBooleanFunction();
					final Boolean valueRight = right.isConstant() ? funcRight.call() : null;

					if (left.isConstant() && right.isConstant()) {
						final Boolean result = compute(valueLeft, valueRight);
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return result;
							}
						};
					} else if (left.isConstant()) {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(valueLeft, funcRight.call());
							}
						};
					} else if (right.isConstant()) {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(funcLeft.call(), valueRight);
							}
						};
					} else {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(funcLeft.call(), funcRight.call());
							}
						};
					}
				} else {
					return null;
				}

			} else if (leftType.equals(ExpressionType.BOOLEAN)) {

				final Callable<Boolean> funcLeft = left.getBooleanFunction();
				final Boolean valueLeft = left.isConstant() ? funcLeft.call() : null;

				if (rightType.equals(ExpressionType.BOOLEAN)) {

					final Callable<Boolean> funcRight = right.getBooleanFunction();
					final Boolean valueRight = right.isConstant() ? funcRight.call() : null;

					if (left.isConstant() && right.isConstant()) {
						final Boolean result = compute(valueLeft, valueRight);
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return result;
							}
						};
					} else if (left.isConstant()) {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(valueLeft, funcRight.call());
							}
						};
					} else if (right.isConstant()) {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(funcLeft.call(), valueRight);
							}
						};
					} else {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(funcLeft.call(), funcRight.call());
							}
						};
					}

				} else if (rightType.equals(ExpressionType.DOUBLE) || rightType.equals(ExpressionType.INTEGER)) {

					final DoubleCallable funcRight = right.getDoubleFunction();
					final double valueRight = right.isConstant() ? funcRight.call() : Double.NaN;

					if (left.isConstant() && right.isConstant()) {
						final Boolean result = compute(valueLeft, valueRight);
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return result;
							}
						};
					} else if (left.isConstant()) {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(valueLeft, funcRight.call());
							}
						};
					} else if (right.isConstant()) {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(funcLeft.call(), valueRight);
							}
						};
					} else {
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return compute(funcLeft.call(), funcRight.call());
							}
						};
					}
				} else {
					return null;
				}

			} else {
				return null;
			}

		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}
	}

	/**
	 * Computes the result for two double values.
	 *
	 * @param left
	 * @param right
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(double left, double right);

	/**
	 * Computes the result for a double value and a boolean value.
	 *
	 * @param left
	 * @param right
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(double left, Boolean right);

	/**
	 * Computes the result for a boolean value and a double value.
	 *
	 * @param left
	 * @param right
	 * @return the result of the computation.
	 */
	protected Boolean compute(Boolean left, double right) {
		return compute(right, left);
	}

	/**
	 * Computes the result for two boolean values.
	 *
	 * @param left
	 * @param right
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(Boolean left, Boolean right);
}
