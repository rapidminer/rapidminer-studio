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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.function.Abstract2DoubleInputFunction;


/**
 * A {@link Function} for addition.
 *
 * @author Gisa Schaefer
 *
 */
public class Plus extends Abstract2DoubleInputFunction {

	/**
	 * The {@link ExpressionType}s that are allowed to be added to a ExpressionType.STRING
	 */
	private static final Set<ExpressionType> STRING_ALLOWED_SET = new HashSet<ExpressionType>(Arrays.asList(
			ExpressionType.STRING, ExpressionType.DOUBLE, ExpressionType.INTEGER, ExpressionType.BOOLEAN));

	/**
	 * Constructs an addition function.
	 */
	public Plus() {
		super("basic.addition", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, Ontology.ATTRIBUTE_VALUE);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 2 && inputEvaluators.length != 1) {
			throw new FunctionInputException("expression_parser.function_wrong_input_two", getFunctionName(), 1, 2,
					inputEvaluators.length);
		}

		ExpressionType type = getResultType(inputEvaluators);

		if (type == ExpressionType.DOUBLE || type == ExpressionType.INTEGER) {
			if (inputEvaluators.length == 1) {
				return inputEvaluators[0];
			} else {
				return new SimpleExpressionEvaluator(makeDoubleCallable(inputEvaluators[0], inputEvaluators[1]), type,
						isResultConstant(inputEvaluators));
			}
		} else {
			// return type is string
			return new SimpleExpressionEvaluator(makeStringCallable(inputEvaluators[0], inputEvaluators[1]), type,
					isResultConstant(inputEvaluators));
		}

	}

	/**
	 * Constructs a String callable from left and right by distinguishing the types of left and
	 * right and using the associated {{@link #compute()} method.
	 *
	 * @param left
	 * @param right
	 * @return
	 */
	private Callable<String> makeStringCallable(ExpressionEvaluator left, ExpressionEvaluator right) {
		try {

			if (left.getType() == ExpressionType.STRING && right.getType() == ExpressionType.STRING) {
				final Callable<String> funcLeft = left.getStringFunction();
				final Callable<String> funcRight = right.getStringFunction();

				final String valueLeft = left.isConstant() ? funcLeft.call() : null;
				final String valueRight = right.isConstant() ? funcRight.call() : null;

				if (left.isConstant() && right.isConstant()) {
					final String result = compute(valueLeft, valueRight);
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return result;
						}

					};
				} else if (left.isConstant()) {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(valueLeft, funcRight.call());
						}

					};
				} else if (right.isConstant()) {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(funcLeft.call(), valueRight);
						}

					};
				} else {
					return new Callable<String>() {

						@Override
						public String call() throws Exception {
							return compute(funcLeft.call(), funcRight.call());
						}

					};
				}
			} else if (left.getType() == ExpressionType.STRING) {
				final Callable<String> funcLeft = left.getStringFunction();
				final String valueLeft = left.isConstant() ? funcLeft.call() : null;

				if (right.getType() == ExpressionType.DOUBLE || right.getType() == ExpressionType.INTEGER) {
					final DoubleCallable funcRight = right.getDoubleFunction();
					final double valueRight = right.isConstant() ? funcRight.call() : Double.NaN;
					final boolean isInteger = right.getType() == ExpressionType.INTEGER;

					if (left.isConstant() && right.isConstant()) {
						final String result = compute(valueLeft, valueRight, isInteger);
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return result;
							}

						};
					} else if (left.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueLeft, funcRight.call(), isInteger);
							}

						};
					} else if (right.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcLeft.call(), valueRight, isInteger);
							}

						};
					} else {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcLeft.call(), funcRight.call(), isInteger);
							}

						};
					}
				} else { // right is boolean
					final Callable<Boolean> funcRight = right.getBooleanFunction();
					final Boolean valueRight = right.isConstant() ? funcRight.call() : null;

					if (left.isConstant() && right.isConstant()) {
						final String result = compute(valueLeft, valueRight);
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return result;
							}

						};
					} else if (left.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueLeft, funcRight.call());
							}

						};
					} else if (right.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcLeft.call(), valueRight);
							}

						};
					} else {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcLeft.call(), funcRight.call());
							}

						};
					}
				}
			} else { // right is STRING
				final Callable<String> funcRight = right.getStringFunction();
				final String valueRight = right.isConstant() ? funcRight.call() : null;

				if (left.getType() == ExpressionType.DOUBLE || left.getType() == ExpressionType.INTEGER) {
					final DoubleCallable funcLeft = left.getDoubleFunction();
					final double valueLeft = left.isConstant() ? funcLeft.call() : Double.NaN;
					final boolean isInteger = left.getType() == ExpressionType.INTEGER;

					if (left.isConstant() && right.isConstant()) {
						final String result = compute(valueLeft, valueRight, isInteger);
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return result;
							}

						};
					} else if (left.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueLeft, funcRight.call(), isInteger);
							}

						};
					} else if (right.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcLeft.call(), valueRight, isInteger);
							}

						};
					} else {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcLeft.call(), funcRight.call(), isInteger);
							}

						};
					}
				} else { // left is boolean
					final Callable<Boolean> funcLeft = left.getBooleanFunction();
					final Boolean valueLeft = left.isConstant() ? funcLeft.call() : null;

					if (left.isConstant() && right.isConstant()) {
						final String result = compute(valueLeft, valueRight);
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return result;
							}

						};
					} else if (left.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(valueLeft, funcRight.call());
							}

						};
					} else if (right.isConstant()) {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcLeft.call(), valueRight);
							}

						};
					} else {
						return new Callable<String>() {

							@Override
							public String call() throws Exception {
								return compute(funcLeft.call(), funcRight.call());
							}

						};
					}
				}
			}
		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}
	}

	/**
	 * Adds two Strings. If both are missing, then the result is missing; if one is missing, it is
	 * ignored.
	 */
	private String compute(String left, String right) {
		if (left == null && right == null) {
			return null;
		}
		return (left == null ? "" : left) + (right == null ? "" : right);
	}

	/**
	 * Adds a String and a double. If the double represents an integer tries to add it without
	 * trailing '.0'. If both are missing, then the result is missing; if one is missing, it is
	 * ignored. If the double is infinite, formats is using a symbol.
	 *
	 * @param left
	 * @param right
	 * @param isInteger
	 *            whether the double represents an {@link ExpressionType#INTEGER}
	 * @return
	 */
	private String compute(String left, double right, boolean isInteger) {
		if (left == null && Double.isNaN(right)) {
			return null;
		}
		if (isInteger && right == (int) right) {
			return (left == null ? "" : left) + (int) right;
		} else if (Double.isInfinite(right)) {
			return (left == null ? "" : left) + Tools.formatNumber(right);
		} else {
			return (left == null ? "" : left) + (Double.isNaN(right) ? "" : right);
		}
	}

	/**
	 * Adds a String and a Boolean. If both are missing, then the result is missing; if one is
	 * missing, it is ignored.
	 */
	private String compute(String left, Boolean right) {
		if (left == null && right == null) {
			return null;
		}
		return (left == null ? "" : left) + (right == null ? "" : right);
	}

	/**
	 * Adds a Boolean and a String. If both are missing, then the result is missing; if one is
	 * missing, it is ignored.
	 */
	private String compute(Boolean left, String right) {
		if (left == null && right == null) {
			return null;
		}
		return (left == null ? "" : left) + (right == null ? "" : right);
	}

	/**
	 * Adds a double and a String. If the double represents an integer tries to add it without
	 * trailing '.0'. If both are missing, then the result is missing; if one is missing, it is
	 * ignored. If the double is infinite, formats is using a symbol.
	 *
	 * @param left
	 * @param right
	 * @param isInteger
	 *            whether the double represents an {@link ExpressionType#INTEGER}
	 * @return
	 */
	private String compute(double left, String right, boolean isInteger) {
		if (Double.isNaN(left) && right == null) {
			return null;
		}
		if (isInteger && left == (int) left) {
			return (int) left + (right == null ? "" : right);
		} else if (Double.isInfinite(left)) {
			return Tools.formatNumber(left) + (right == null ? "" : right);
		} else {
			return (Double.isNaN(left) ? "" : left) + (right == null ? "" : right);
		}
	}

	@Override
	protected double compute(double value1, double value2) {
		return value1 + value2;
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType firstType = inputTypes[0];
		if (inputTypes.length == 1) {
			if (firstType == ExpressionType.INTEGER || firstType == ExpressionType.DOUBLE) {
				return firstType;
			} else {
				throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "numerical");
			}
		} else {
			ExpressionType secondType = inputTypes[1];
			if (firstType == ExpressionType.INTEGER && secondType == ExpressionType.INTEGER) {
				return ExpressionType.INTEGER;
			} else if ((firstType == ExpressionType.INTEGER || firstType == ExpressionType.DOUBLE)
					&& (secondType == ExpressionType.INTEGER || secondType == ExpressionType.DOUBLE)) {
				return ExpressionType.DOUBLE;
			} else if ((firstType == ExpressionType.STRING || secondType == ExpressionType.STRING)
					&& STRING_ALLOWED_SET.contains(firstType) && STRING_ALLOWED_SET.contains(secondType)) {
				return ExpressionType.STRING;
			} else {
				throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(),
						"numerical or nominal");
			}
		}

	}

}
