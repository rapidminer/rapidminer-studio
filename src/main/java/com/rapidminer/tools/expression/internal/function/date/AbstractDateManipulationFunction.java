/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
import java.util.concurrent.atomic.AtomicBoolean;

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
		return compute(() -> null, inputEvaluators);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionEvaluator... inputEvaluators) {
		ExpressionType type = getResultType(inputEvaluators);

		ExpressionEvaluator locale = new SimpleExpressionEvaluator((String) null, ExpressionType.STRING);
		ExpressionEvaluator timeZone = new SimpleExpressionEvaluator((String) null, ExpressionType.STRING);
		ExpressionEvaluator date = inputEvaluators[0];
		ExpressionEvaluator value = inputEvaluators[1];
		ExpressionEvaluator unit = inputEvaluators[2];
		if (inputEvaluators.length != 3) {
			locale = inputEvaluators[3];
			timeZone = inputEvaluators[4];
		}
		return new SimpleExpressionEvaluator(type, makeDateCallable(stopChecker, date, value, unit, locale, timeZone),
				isResultConstant(inputEvaluators));

	}

	private Callable<Date> makeDateCallable(Callable<Void> stopChecker, ExpressionEvaluator date, ExpressionEvaluator value, ExpressionEvaluator unit,
											ExpressionEvaluator locale, ExpressionEvaluator timeZone) {
		final Callable<Date> funcDate;
		final DoubleCallable funcValue;
		final Callable<String> funcUnit;
		final Callable<String> funcLocale;
		final Callable<String> funcTimeZone;
		AtomicBoolean allConstant = new AtomicBoolean(true);
		try {
			if (date.isConstant()) {
				Date dateValue = date.getDateFunction().call();
				funcDate = () -> dateValue;
			} else {
				funcDate = date.getDateFunction();
				allConstant.set(false);
			}
			if (value.isConstant()) {
				double valueValue = value.getDoubleFunction().call();
				funcValue = () -> valueValue;
			} else {
				funcValue = value.getDoubleFunction();
				allConstant.set(false);
			}
			funcUnit = getStringCallable(unit, allConstant);
			funcLocale = getStringCallable(locale, allConstant);
			funcTimeZone = getStringCallable(timeZone, allConstant);

			Callable<Date> callable = () -> compute(stopChecker, funcDate.call(), funcValue.call(), funcUnit.call(),
					funcLocale.call(), funcTimeZone.call());
			// all constant values
			if (allConstant.get()) {
				Date resultDate = callable.call();
				return () -> resultDate;
			}
			return callable;
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
	 * 		date to manipulate
	 * @param value
	 * 		the amount of which the date should change
	 * @param unit
	 * 		the unit constant which should be changed
	 * @param valueLocale
	 * 		the locale string
	 * @param valueTimezone
	 * 		time zone string
	 * @return the result of the computation.
	 */
	protected abstract Date compute(Date date, double value, String unit, String valueLocale, String valueTimezone);

	/**
	 * Computes the result for manipulating a date for a certain value on a given unit with
	 * additional locale and time zone arguments.
	 *
	 * @param stopChecker
	 * 		optional callable to check for stop
	 * @param date
	 * 		date to manipulate
	 * @param value
	 * 		the amount of which the date should change
	 * @param unit
	 * 		the unit constant which should be changed
	 * @param valueLocale
	 * 		the locale string
	 * @param valueTimezone
	 * 		time zone string
	 * @return the result of the computation.
	 * @since 9.6.0
	 */
	protected Date compute(Callable<Void> stopChecker, Date date, double value, String unit, String valueLocale, String valueTimezone) {
		return compute(date, value, unit, valueLocale, valueTimezone);
	}

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
		if (secondType != ExpressionType.DOUBLE && secondType != ExpressionType.INTEGER) {
			throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(), "double or integer",
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

	/** @since 9.6.0 */
	private static Callable<String> getStringCallable(ExpressionEvaluator stringEvaluator, AtomicBoolean allConstant) throws Exception {
		if (stringEvaluator.isConstant()) {
			String unitValue = stringEvaluator.getStringFunction().call();
			return () -> unitValue;
		}
		allConstant.set(false);
		return stringEvaluator.getStringFunction();
	}
}
