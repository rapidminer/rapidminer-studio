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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.DoubleCallable;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionParsingException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.ExpressionParserConstants;
import com.rapidminer.tools.expression.internal.SimpleExpressionEvaluator;
import com.rapidminer.tools.expression.internal.function.AbstractFunction;


/**
 * A {@link Function} for getting part of a date.
 *
 * @author David Arnu
 *
 */
public class DateGet extends AbstractFunction {

	public DateGet() {
		super("date.date_get", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, Ontology.INTEGER);
	}

	@Override
	public ExpressionEvaluator compute(ExpressionEvaluator... inputEvaluators) {
		ExpressionType type = getResultType(inputEvaluators);

		if (inputEvaluators.length == 2) {

			ExpressionEvaluator date = inputEvaluators[0];
			ExpressionEvaluator unit = inputEvaluators[1];
			return new SimpleExpressionEvaluator(makeDoubleCallable(date, unit, null, null), type,
					isResultConstant(inputEvaluators));
		} else {
			ExpressionEvaluator date = inputEvaluators[0];
			ExpressionEvaluator unit = inputEvaluators[1];
			ExpressionEvaluator locale = inputEvaluators[2];
			ExpressionEvaluator timeZone = inputEvaluators[3];
			return new SimpleExpressionEvaluator(makeDoubleCallable(date, unit, locale, timeZone), type,
					isResultConstant(inputEvaluators));
		}

	}

	private DoubleCallable makeDoubleCallable(ExpressionEvaluator date, ExpressionEvaluator unit,
			ExpressionEvaluator locale, ExpressionEvaluator timeZone) {
		final Callable<Date> funcDate = date.getDateFunction();
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
			final String valueUnit = unit.isConstant() ? funcUnit.call() : null;
			final String valueLocale = locale.isConstant() ? funcLocale.call() : null;
			final String valueTimezone = timeZone.isConstant() ? funcTimeZone.call() : null;

			// only likely cases checked:

			// all constant values
			if (date.isConstant() && unit.isConstant() && locale.isConstant() && timeZone.isConstant()) {
				final double result = compute(valueDate, valueUnit, valueLocale, valueTimezone);

				return new DoubleCallable() {

					@Override
					public double call() throws Exception {
						// TODO Auto-generated method stub
						return result;
					}
				};
				// constant date and unit is not constant
			} else if (date.isConstant() && !unit.isConstant()) {
				// branch with constant locale and time zone data, probably both are constant or
				// both are not
				if (locale.isConstant() && timeZone.isConstant()) {
					return new DoubleCallable() {

						@Override
						public double call() throws Exception {
							// TODO Auto-generated method stub
							return compute(valueDate, funcUnit.call(), valueLocale, valueTimezone);
						}
					};
				} else {
					return new DoubleCallable() {

						@Override
						public double call() throws Exception {
							// TODO Auto-generated method stub
							return compute(valueDate, funcUnit.call(), funcLocale.call(), funcTimeZone.call());
						}
					};
				}
			} else if (!date.isConstant() && unit.isConstant()) {
				// branch with constant locale and time zone data, probably both are constant or
				// both are not
				if (locale.isConstant() && timeZone.isConstant()) {
					return new DoubleCallable() {

						@Override
						public double call() throws Exception {
							// TODO Auto-generated method stub
							return compute(funcDate.call(), valueUnit, valueLocale, valueTimezone);
						}
					};
				} else {
					return new DoubleCallable() {

						@Override
						public double call() throws Exception {
							// TODO Auto-generated method stub
							return compute(funcDate.call(), valueUnit, funcLocale.call(), funcTimeZone.call());
						}
					};
				}
				// date and unit are not constant
			} else {
				// branch with constant locale and time zone data, probably both are constant or
				// both are not
				if (locale.isConstant() && timeZone.isConstant()) {
					return new DoubleCallable() {

						@Override
						public double call() throws Exception {
							// TODO Auto-generated method stub
							return compute(funcDate.call(), funcUnit.call(), valueLocale, valueTimezone);
						}
					};
				} else {
					return new DoubleCallable() {

						@Override
						public double call() throws Exception {
							// TODO Auto-generated method stub
							return compute(funcDate.call(), funcUnit.call(), funcLocale.call(), funcTimeZone.call());
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
	 *
	 * @param date
	 * @param unit
	 * @param locale
	 * @param timeZone
	 * @return the value of the selected unit of the given date
	 */
	protected double compute(Date date, String unit, String valueLocale, String valueTimeZone) {
		Locale locale;
		TimeZone zone;
		if (valueLocale == null) {
			locale = Locale.getDefault();
		} else {
			locale = new Locale(valueLocale);
		}
		if (valueTimeZone == null) {
			zone = TimeZone.getDefault();
		} else {
			zone = TimeZone.getTimeZone(valueTimeZone);
		}

		// for missing values as arguments, a missing value is returned
		if (date == null || unit == null) {
			return Double.NaN;
		}

		Calendar cal = Calendar.getInstance(zone, locale);
		cal.setTime(date);
		double result;

		switch (unit) {
			case ExpressionParserConstants.DATE_UNIT_YEAR:
				result = cal.get(Calendar.YEAR);
				break;
			case ExpressionParserConstants.DATE_UNIT_MONTH:
				result = cal.get(Calendar.MONTH);
				break;
			case ExpressionParserConstants.DATE_UNIT_WEEK:
				result = cal.get(Calendar.WEEK_OF_YEAR);
				break;

			case ExpressionParserConstants.DATE_UNIT_DAY:
				result = cal.get(Calendar.DAY_OF_MONTH);
				break;

			case ExpressionParserConstants.DATE_UNIT_HOUR:
				result = cal.get(Calendar.HOUR_OF_DAY);
				break;
			case ExpressionParserConstants.DATE_UNIT_MINUTE:
				result = cal.get(Calendar.MINUTE);
				break;
			case ExpressionParserConstants.DATE_UNIT_SECOND:
				result = cal.get(Calendar.SECOND);
				break;
			case ExpressionParserConstants.DATE_UNIT_MILLISECOND:
				result = cal.get(Calendar.MILLISECOND);
				break;
			default:
				throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(),
						"unit constant", "second");

		}

		return result;
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		if (inputTypes.length != 2 && inputTypes.length != 4) {
			throw new FunctionInputException("expression_parser.function_wrong_input_two", getFunctionName(), "2", "4",
					inputTypes.length);
		}
		ExpressionType firstType = inputTypes[0];
		ExpressionType secondType = inputTypes[1];

		if (firstType != ExpressionType.DATE) {
			throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "date", "first");
		}
		if (secondType != ExpressionType.STRING) {
			throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "string",
					"second");
		}

		if (inputTypes.length == 4) {
			if (inputTypes[2] != ExpressionType.STRING) {
				throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "string",
						"third");
			}
			if (inputTypes[3] != ExpressionType.STRING) {
				throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "string",
						"fourth");
			}
		}
		return ExpressionType.INTEGER;

	}

}
