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
package com.rapidminer.tools.expression.internal.function.comparison;

import java.util.concurrent.Callable;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.function.AbstractFunction;


/**
 * Abstract class for a comparison function that has 2 numerical or nominal inputs, but both of the
 * same type
 *
 * @author Sabrina Kirstein
 *
 */
public abstract class AbstractComparisonFunctionWith2Inputs extends AbstractFunction {

	/**
	 * Constructs a comparison AbstractFunction with {@link FunctionDescription} generated from the
	 * arguments and the function name generated from the description.
	 *
	 * @param i18nKey
	 *            the key for the {@link FunctionDescription}. The functionName is read from
	 *            "gui.dialog.function.i18nKey.name", the helpTextName from ".help", the groupName
	 *            from ".group", the description from ".description" and the function with
	 *            parameters from ".parameters". If ".parameters" is not present, the ".name" is
	 *            taken for the function with parameters.
	 */
	public AbstractComparisonFunctionWith2Inputs(String i18nKey) {
		super(i18nKey, 2, Ontology.BINOMINAL);
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
	 * Builds a boolean callable from evaluator using {@link #compute(double, double)} or
	 * {@link #compute(String, String)}, where constant child results are evaluated.
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
			// if both types are numeric
			if ((leftType.equals(ExpressionType.DOUBLE) || leftType.equals(ExpressionType.INTEGER))
					&& (rightType.equals(ExpressionType.DOUBLE) || rightType.equals(ExpressionType.INTEGER))) {

				final DoubleCallable funcLeft = left.getDoubleFunction();
				final double valueLeft = left.isConstant() ? funcLeft.call() : Double.NaN;
				final DoubleCallable funcRight = right.getDoubleFunction();
				final double valueRight = right.isConstant() ? funcRight.call() : Double.NaN;

				if (left.isConstant() && right.isConstant()) {
					final boolean result = compute(valueLeft, valueRight);
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

				// if both types are nominal
			} else if (leftType.equals(ExpressionType.STRING) && rightType.equals(ExpressionType.STRING)) {

				final Callable<String> funcLeft = left.getStringFunction();
				final String valueLeft = left.isConstant() ? funcLeft.call() : null;

				final Callable<String> funcRight = right.getStringFunction();
				final String valueRight = right.isConstant() ? funcRight.call() : null;

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
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(double left, double right);

	/**
	 * Computes the result for two String values.
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(String left, String right);

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		if (inputTypes.length != 2) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), "2",
					inputTypes.length);
		}
		ExpressionType type1 = inputTypes[0];
		ExpressionType type2 = inputTypes[1];

		// types have to be the same
		if (type1 != type2) {
			if (!((type1 == ExpressionType.INTEGER || type1 == ExpressionType.DOUBLE) && (type2 == ExpressionType.INTEGER || type2 == ExpressionType.DOUBLE))) {
				throw new FunctionInputException("expression_parser.function_needs_same_type", getFunctionName());
			}
		}

		// type has to be numerical or nominal
		for (ExpressionType inputType : inputTypes) {
			if (inputType != ExpressionType.INTEGER && inputType != ExpressionType.DOUBLE
					&& inputType != ExpressionType.STRING) {
				throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(),
						"nominal or numerical");
			}
		}
		// result is always boolean
		return ExpressionType.BOOLEAN;
	}
}
