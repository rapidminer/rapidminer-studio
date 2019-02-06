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

import java.util.Date;
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
 * Abstract class for a equality check function that has two arbitrary inputs
 *
 * @author Sabrina Kirstein
 */
public abstract class AbstractEqualityFunctionWith2Inputs extends AbstractFunction {

	/**
	 * Constructs an equality check Function with 2 parameters with {@link FunctionDescription}
	 */
	public AbstractEqualityFunctionWith2Inputs(String i18nKey) {
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
	 * Builds a boolean callable from two {@link ExpressionEvaluator}s, where constant child results
	 * are evaluated.
	 *
	 * @param left
	 *            {@link ExpressionEvaluator}
	 * @param right
	 *            {@link ExpressionEvaluator}
	 * @return the resulting boolean callable
	 */
	protected Callable<Boolean> makeBooleanCallable(ExpressionEvaluator left, ExpressionEvaluator right) {

		ExpressionType leftType = left.getType();
		ExpressionType rightType = right.getType();

		// Any type checked against any type is allowed
		try {
			switch (leftType) {
				case INTEGER:
				case DOUBLE:
					final DoubleCallable funcLeftDouble = left.getDoubleFunction();
					final double valueLeftDouble = left.isConstant() ? funcLeftDouble.call() : Double.NaN;

					switch (rightType) {
						case INTEGER:
						case DOUBLE:
							final DoubleCallable funcRightDouble = right.getDoubleFunction();
							final double valueRightDouble = right.isConstant() ? funcRightDouble.call() : Double.NaN;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftDouble, valueRightDouble);
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
										return compute(valueLeftDouble, funcRightDouble.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDouble.call(), valueRightDouble);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDouble.call(), funcRightDouble.call());
									}
								};
							}
						case STRING:
							final Callable<String> funcRightString = right.getStringFunction();
							final String valueRightString = right.isConstant() ? funcRightString.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftDouble, valueRightString);
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
										return compute(valueLeftDouble, funcRightString.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDouble.call(), valueRightString);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDouble.call(), funcRightString.call());
									}
								};
							}
						case DATE:
							final Callable<Date> funcRightDate = right.getDateFunction();
							final Date valueRightDate = right.isConstant() ? funcRightDate.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftDouble, valueRightDate);
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
										return compute(valueLeftDouble, funcRightDate.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDouble.call(), valueRightDate);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDouble.call(), funcRightDate.call());
									}
								};
							}
						case BOOLEAN:
							final Callable<Boolean> funcRightBoolean = right.getBooleanFunction();
							final Boolean valueRightBoolean = right.isConstant() ? funcRightBoolean.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftDouble, valueRightBoolean);
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
										return compute(valueLeftDouble, funcRightBoolean.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDouble.call(), valueRightBoolean);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDouble.call(), funcRightBoolean.call());
									}
								};
							}
						default:
							return null;
					}
				case STRING:
					final Callable<String> funcLeftString = left.getStringFunction();
					final String valueLeftString = left.isConstant() ? funcLeftString.call() : null;

					switch (rightType) {
						case INTEGER:
						case DOUBLE:
							final DoubleCallable funcRightDouble = right.getDoubleFunction();
							final double valueRightDouble = right.isConstant() ? funcRightDouble.call() : Double.NaN;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftString, valueRightDouble);
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
										return compute(valueLeftString, funcRightDouble.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftString.call(), valueRightDouble);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftString.call(), funcRightDouble.call());
									}
								};
							}
						case STRING:
							final Callable<String> funcRightString = right.getStringFunction();
							final String valueRightString = right.isConstant() ? funcRightString.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftString, valueRightString);
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
										return compute(valueLeftString, funcRightString.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftString.call(), valueRightString);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftString.call(), funcRightString.call());
									}
								};
							}
						case DATE:
							final Callable<Date> funcRightDate = right.getDateFunction();
							final Date valueRightDate = right.isConstant() ? funcRightDate.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftString, valueRightDate);
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
										return compute(valueLeftString, funcRightDate.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftString.call(), valueRightDate);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftString.call(), funcRightDate.call());
									}
								};
							}
						case BOOLEAN:
							final Callable<Boolean> funcRightBoolean = right.getBooleanFunction();
							final Boolean valueRightBoolean = right.isConstant() ? funcRightBoolean.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftString, valueRightBoolean);
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
										return compute(valueLeftString, funcRightBoolean.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftString.call(), valueRightBoolean);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftString.call(), funcRightBoolean.call());
									}
								};
							}
						default:
							return null;
					}
				case DATE:
					final Callable<Date> funcLeftDate = left.getDateFunction();
					final Date valueLeftDate = left.isConstant() ? funcLeftDate.call() : null;

					switch (rightType) {
						case INTEGER:
						case DOUBLE:
							final DoubleCallable funcRightDouble = right.getDoubleFunction();
							final double valueRightDouble = right.isConstant() ? funcRightDouble.call() : Double.NaN;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftDate, valueRightDouble);
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
										return compute(valueLeftDate, funcRightDouble.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDate.call(), valueRightDouble);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDate.call(), funcRightDouble.call());
									}
								};
							}
						case STRING:
							final Callable<String> funcRightString = right.getStringFunction();
							final String valueRightString = right.isConstant() ? funcRightString.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftDate, valueRightString);
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
										return compute(valueLeftDate, funcRightString.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDate.call(), valueRightString);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDate.call(), funcRightString.call());
									}
								};
							}
						case DATE:
							final Callable<Date> funcRightDate = right.getDateFunction();
							final Date valueRightDate = right.isConstant() ? funcRightDate.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftDate, valueRightDate);
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
										return compute(valueLeftDate, funcRightDate.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDate.call(), valueRightDate);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDate.call(), funcRightDate.call());
									}
								};
							}
						case BOOLEAN:
							final Callable<Boolean> funcRightBoolean = right.getBooleanFunction();
							final Boolean valueRightBoolean = right.isConstant() ? funcRightBoolean.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftDate, valueRightBoolean);
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
										return compute(valueLeftDate, funcRightBoolean.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDate.call(), valueRightBoolean);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftDate.call(), funcRightBoolean.call());
									}
								};
							}
						default:
							return null;
					}
				case BOOLEAN:
					final Callable<Boolean> funcLeftBoolean = left.getBooleanFunction();
					final Boolean valueLeftBoolean = left.isConstant() ? funcLeftBoolean.call() : null;

					switch (rightType) {
						case INTEGER:
						case DOUBLE:
							final DoubleCallable funcRightDouble = right.getDoubleFunction();
							final double valueRightDouble = right.isConstant() ? funcRightDouble.call() : Double.NaN;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftBoolean, valueRightDouble);
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
										return compute(valueLeftBoolean, funcRightDouble.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftBoolean.call(), valueRightDouble);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftBoolean.call(), funcRightDouble.call());
									}
								};
							}
						case STRING:
							final Callable<String> funcRightString = right.getStringFunction();
							final String valueRightString = right.isConstant() ? funcRightString.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftBoolean, valueRightString);
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
										return compute(valueLeftBoolean, funcRightString.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftBoolean.call(), valueRightString);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftBoolean.call(), funcRightString.call());
									}
								};
							}
						case DATE:
							final Callable<Date> funcRightDate = right.getDateFunction();
							final Date valueRightDate = right.isConstant() ? funcRightDate.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftBoolean, valueRightDate);
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
										return compute(valueLeftBoolean, funcRightDate.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftBoolean.call(), valueRightDate);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftBoolean.call(), funcRightDate.call());
									}
								};
							}
						case BOOLEAN:
							final Callable<Boolean> funcRightBoolean = right.getBooleanFunction();
							final Boolean valueRightBoolean = right.isConstant() ? funcRightBoolean.call() : null;
							if (left.isConstant() && right.isConstant()) {
								final Boolean result = compute(valueLeftBoolean, valueRightBoolean);
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
										return compute(valueLeftBoolean, funcRightBoolean.call());
									}
								};
							} else if (right.isConstant()) {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftBoolean.call(), valueRightBoolean);
									}
								};
							} else {
								return new Callable<Boolean>() {

									@Override
									public Boolean call() throws Exception {
										return compute(funcLeftBoolean.call(), funcRightBoolean.call());
									}
								};
							}
						default:
							return null;
					}
				default:
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
	 * Computes the result for a double and a boolean value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(double left, Boolean right);

	/**
	 * Computes the result for a boolean and a double value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected Boolean compute(Boolean left, double right) {
		return compute(right, left);
	}

	/**
	 * Computes the result for a double and a String value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(double left, String right);

	/**
	 * Computes the result for a String and a double value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected Boolean compute(String left, double right) {
		return compute(right, left);
	}

	/**
	 * Computes the result for a double value and a date
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(double left, Date right);

	/**
	 * Computes the result for a date value and a double value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected Boolean compute(Date left, double right) {
		return compute(right, left);
	}

	/**
	 * Computes the result for two boolean values.
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(Boolean left, Boolean right);

	/**
	 * Computes the result for a boolean and a String value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(Boolean left, String right);

	/**
	 * Computes the result for a String value and a boolean value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected Boolean compute(String left, Boolean right) {
		return compute(right, left);
	}

	/**
	 * Computes the result for a boolean value and a String value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(Boolean left, Date right);

	/**
	 * Computes the result for a date value and a boolean value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected Boolean compute(Date left, Boolean right) {
		return compute(right, left);
	}

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

	/**
	 * Computes the result for a String value and a Date value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(String left, Date right);

	/**
	 * Computes the result for a Date value and a String value
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected Boolean compute(Date left, String right) {
		return compute(right, left);
	}

	/**
	 * Computes the result for two Date values.
	 *
	 * @param left
	 *            value
	 * @param right
	 *            value
	 * @return the result of the computation.
	 */
	protected abstract Boolean compute(Date left, Date right);

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		if (inputTypes.length != 2) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), "2",
					inputTypes.length);
		}
		// result is always boolean
		return ExpressionType.BOOLEAN;
	}
}
