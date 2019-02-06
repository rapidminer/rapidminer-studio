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
package com.rapidminer.tools.expression.internal.function.basic;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.function.Abstract2DoubleInputFunction;


/**
 * A {@link Function} for subtraction.
 *
 * @author Gisa Schaefer
 *
 */
public class Minus extends Abstract2DoubleInputFunction {

	/**
	 * Constructs a subtraction function.
	 */
	public Minus() {
		super("basic.subtraction", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, Ontology.NUMERICAL);

	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 2 && inputEvaluators.length != 1) {
			throw new FunctionInputException("expression_parser.function_wrong_input_two", getFunctionName(), 1, 2,
					inputEvaluators.length);
		}

		ExpressionType type = getResultType(inputEvaluators);
		DoubleCallable func;

		if (inputEvaluators.length == 1) {
			func = makeDoubleCallable(inputEvaluators[0]);
		} else {
			func = makeDoubleCallable(inputEvaluators[0], inputEvaluators[1]);
		}

		return new SimpleExpressionEvaluator(func, type, isResultConstant(inputEvaluators));
	}

	/**
	 * Creates the callable for one double callable input.
	 *
	 * @param expressionEvaluator
	 * @return
	 */
	private DoubleCallable makeDoubleCallable(ExpressionEvaluator input) {
		final DoubleCallable inputFunction = input.getDoubleFunction();
		try {
			if (input.isConstant()) {
				final double inputValue = inputFunction.call();
				final double returnValue = -inputValue;
				return new DoubleCallable() {

					@Override
					public double call() throws Exception {
						return returnValue;
					}

				};
			} else {
				return new DoubleCallable() {

					@Override
					public double call() throws Exception {
						return -inputFunction.call();
					}

				};
			}
		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType firstType = inputTypes[0];
		if (firstType == ExpressionType.INTEGER && (inputTypes.length == 1 || inputTypes[1] == ExpressionType.INTEGER)) {
			return ExpressionType.INTEGER;
		} else if ((firstType == ExpressionType.INTEGER || firstType == ExpressionType.DOUBLE)
				&& (inputTypes.length == 1 || inputTypes[1] == ExpressionType.INTEGER || inputTypes[1] == ExpressionType.DOUBLE)) {
			return ExpressionType.DOUBLE;
		} else {
			throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "numerical");
		}
	}

	@Override
	protected double compute(double value1, double value2) {
		return value1 - value2;
	}

}
