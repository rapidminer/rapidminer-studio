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
package com.rapidminer.tools.expression.internal.function.date;

import java.util.Date;
import java.util.concurrent.Callable;

import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.function.AbstractFunction;


/**
 * Abstract class for a {@link Function} that has a date, an integer and a unit as arguments and can
 * have locale and returns a date. The function can has time zone and localization strings as
 * optional parameters
 *
 * @author David Arnu
 *
 */
public abstract class AbstractDateManipulationFunction extends AbstractFunction {

	public AbstractDateManipulationFunction(String i18nKey, int numberOfArgumentsToCheck, int returnType) {
		super(i18nKey, numberOfArgumentsToCheck, returnType);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {
		ExpressionType type = getResultType(inputEvaluators);

		if (inputEvaluators.length == 3) {

			ExpressionEvaluator date = inputEvaluators[0];
			ExpressionEvaluator value = inputEvaluators[1];
			ExpressionEvaluator unit = inputEvaluators[2];

			return new SimpleExpressionEvaluator(type, makeDateCallable(date, value, unit, null, null),
					isResultConstant(inputEvaluators));
		} else {
			ExpressionEvaluator date = inputEvaluators[0];
			ExpressionEvaluator value = inputEvaluators[1];
			ExpressionEvaluator unit = inputEvaluators[2];
			ExpressionEvaluator locale = inputEvaluators[3];
			ExpressionEvaluator timeZone = inputEvaluators[4];
			return new SimpleExpressionEvaluator(type, makeDateCallable(date, value, unit, locale, timeZone),
					isResultConstant(inputEvaluators));
		}

	}

	private Callable<Date> makeDateCallable(ExpressionEvaluator date, ExpressionEvaluator value, ExpressionEvaluator unit,
			ExpressionEvaluator locale, ExpressionEvaluator timeZone) {
		final Callable<Date> funcDate = date.getDateFunction();
		final DoubleCallable funcValue = value.getDoubleFunction();
		final Callable<String> funcUnit = unit.getStringFunction();
		final Callable<String> funcLocale;
		final Callable<String> funcTimeZone;

		if (locale != null) {
			funcLocale = locale.getStringFunction();
		} else {
			// create an dummy ExpressionEvaluator for the missing locale argument
			locale = new SimpleExpressionEvaluator("", ExpressionType.STRING);
			funcLocale = new Callable<String>() {

				@Override
				public String call() throws Exception {
					return null;
				}
			};
		}
		if (timeZone != null) {
			funcTimeZone = timeZone.getStringFunction();
		} else {
			// create an dummy ExpressionEvaluator for the missing time zone argument
			timeZone = new SimpleExpressionEvaluator("", ExpressionType.STRING);
			funcTimeZone = new Callable<String>() {

				@Override
				public String call() throws Exception {
					return null;
				}
			};
		}
		try {
			final Date valueDate = date.isConstant() ? funcDate.call() : null;
			final double valueValue = value.isConstant() ? funcValue.call() : Double.NaN;
			final String valueUnit = unit.isConstant() ? funcUnit.call() : null;
			final String valueLocale = locale.isConstant() ? funcLocale.call() : null;
			final String valueTimezone = timeZone.isConstant() ? funcTimeZone.call() : null;

			// only likely cases checked:

			// all constant values
			if (date.isConstant() && value.isConstant() && unit.isConstant() && locale.isConstant() && timeZone.isConstant()) {
				final Date result = compute(valueDate, valueValue, valueUnit, valueLocale, valueTimezone);

				return new Callable<Date>() {

					@Override
					public Date call() throws Exception {
						return result;
					}

				};
				// constant date, value and unit are not constant
			} else if (date.isConstant() && !value.isConstant() && !unit.isConstant()) {
				// branch with constant locale and time zone data, probably both are constant or
				// both are not
				if (locale.isConstant() && timeZone.isConstant()) {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(valueDate, funcValue.call(), funcUnit.call(), valueLocale, valueTimezone);
						}
					};
				} else {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(valueDate, funcValue.call(), funcUnit.call(), funcLocale.call(),
									funcTimeZone.call());
						}
					};
				}
				// constant value, date and unit are not
			} else if (!date.isConstant() && value.isConstant() && !unit.isConstant()) {
				// branch with constant locale and time zone data, probably both are constant or
				// both are not
				if (locale.isConstant() && timeZone.isConstant()) {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcDate.call(), valueValue, funcUnit.call(), valueLocale, valueTimezone);
						}
					};
				} else {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcDate.call(), valueValue, funcUnit.call(), funcLocale.call(),
									funcTimeZone.call());
						}
					};
				}
				// constant unit, date and value are not
			} else if (!date.isConstant() && !value.isConstant() && unit.isConstant()) {
				// branch with constant locale and time zone data, probably both are constant or
				// both are not
				if (locale.isConstant() && timeZone.isConstant()) {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcDate.call(), funcValue.call(), valueUnit, valueLocale, valueTimezone);
						}
					};
				} else {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcDate.call(), funcValue.call(), valueUnit, funcLocale.call(),
									funcTimeZone.call());
						}
					};
				}
				// value and unit are constant, date is not
			} else if (!date.isConstant() && value.isConstant() && unit.isConstant()) {
				// branch with constant locale and time zone data, probably both are constant or
				// both are not
				if (locale.isConstant() && timeZone.isConstant()) {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcDate.call(), valueValue, valueUnit, valueLocale, valueTimezone);
						}
					};
				} else {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcDate.call(), valueValue, valueUnit, funcLocale.call(), funcTimeZone.call());
						}
					};
				}
				// date, value and unit are variable
			} else {
				// branch with constant locale and time zone data, probably both are constant or
				// both are not
				if (locale.isConstant() && timeZone.isConstant()) {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcDate.call(), funcValue.call(), funcUnit.call(), valueLocale, valueTimezone);
						}
					};
				} else {
					return new Callable<Date>() {

						@Override
						public Date call() throws Exception {
							return compute(funcDate.call(), funcValue.call(), funcUnit.call(), funcLocale.call(),
									funcTimeZone.call());
						}
					};
				}
			}

		} catch (ExpressionParsingException e) {
			throw e;
		} catch (Exception e) {
			throw new ExpressionParsingException(e);
		}
	}

	/**
	 * Computes the result for manipulating a date for a certain value on a given unit with
	 * additional locale and time zone arguments.
	 *
	 * @param date
	 *            date to manipulate
	 * @param value
	 *            the amount of which the date should change
	 * @param unit
	 *            the unit constant which should be changed
	 * @param valueTimezone
	 *            time zone string
	 * @return the result of the computation.
	 */
	protected abstract Date compute(Date date, double value, String unit, String valueLocale, String valueTimezone);

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		if (inputTypes.length != 3 && inputTypes.length != 5) {
			throw new FunctionInputException("expression_parser.function_wrong_input_two", getFunctionName(), "3", "5",
					inputTypes.length);
		}
		ExpressionType firstType = inputTypes[0];
		ExpressionType secondType = inputTypes[1];
		ExpressionType thirdType = inputTypes[2];

		if (firstType != ExpressionType.DATE) {
			throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "date", "first");
		}
		if (secondType != ExpressionType.INTEGER) {
			throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "integer",
					"second");

		}
		if (thirdType != ExpressionType.STRING) {
			throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "string",
					"third");

		}

		if (inputTypes.length == 5) {
			if (inputTypes[3] != ExpressionType.STRING) {
				throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "string",
						"fourth");
			}
			if (inputTypes[4] != ExpressionType.STRING) {
				throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "string",
						"fifth");
			}

		}
		return ExpressionType.DATE;

	}
}
