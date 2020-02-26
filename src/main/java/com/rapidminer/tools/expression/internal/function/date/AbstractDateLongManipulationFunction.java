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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.ExpressionParserConstants;


/**
 * Abstract base for adding/setting a date
 *
 * @author Jan Czogalla, David Arnu
 * @since 9.6.0
 */
public abstract class AbstractDateLongManipulationFunction extends AbstractDateManipulationFunction {

	/** maximum that can be properly processed in {@link Calendar#add(int, int)} or {@link Calendar#set(int, int)} calls */
	static final int MAX_DATE_VALUE = Integer.MAX_VALUE - 10000;

	AbstractDateLongManipulationFunction(String i18nKey, int numberOfArgumentsToCheck, int returnType) {
		super(i18nKey, numberOfArgumentsToCheck, returnType);
	}

	@Override
	protected Date compute(Date date, double value, String unit, String valueLocale, String valueTimezone) {
		return compute(() -> null, date, value, unit, valueLocale, valueTimezone);
	}

	@Override
	protected Date compute(Callable<Void> stopChecker, Date date, double value, String unit, String valueLocale, String valueTimezone) {

		Locale locale;
		TimeZone zone;
		if (valueLocale == null) {
			locale = Locale.getDefault();
		} else {
			locale = new Locale(valueLocale);
		}
		if (valueTimezone == null) {
			zone = TimeZone.getDefault();
		} else {
			zone = TimeZone.getTimeZone(valueTimezone);
		}

		// for missing values as arguments, a missing value is returned
		if (date == null || unit == null || Double.isNaN(value) || Double.isInfinite(value)) {
			return null;
		}

		Calendar cal = Calendar.getInstance(zone, locale);
		cal.setTime(date);
		long dateValue = (long) value;
		int sign = dateValue < 0 ? -1 : 1;
		dateValue *= sign;
		int dateUnit = mapUnitNameToIndex(unit);
		if (dateValue <= MAX_DATE_VALUE) {
			integerManipulation(stopChecker, cal, dateUnit, (int) (dateValue * sign));
		} else {
			longManipulation(stopChecker, cal, dateValue, sign, dateUnit);
		}


		return cal.getTime();
	}

	/**
	 * Integer date manipulation; this sets/adds the given integer value of the specified calendar field.
	 * Subclasses must implemented this method accordingly.
	 */
	abstract void integerManipulation(Callable<Void> stopChecker, Calendar cal, int unit, int value);

	/**
	 * Manipulate date when the absolute value parsed exceeds {@link #MAX_DATE_VALUE}. Might call
	 * {@link #integerManipulation(Callable, Calendar, int, int)} first with a modified {@code dateValue}.
	 */
	abstract void longManipulation(Callable<Void> stopChecker, Calendar cal, long dateValue, int sign, int dateUnit);

	/** Maps the name of the unit to an integer unit identifier of {@link Calendar} */
	private int mapUnitNameToIndex(String unit) {
		switch (unit) {
			case ExpressionParserConstants.DATE_UNIT_YEAR:
				return Calendar.YEAR;
			case ExpressionParserConstants.DATE_UNIT_MONTH:
				return Calendar.MONTH;
			case ExpressionParserConstants.DATE_UNIT_WEEK:
				return Calendar.WEEK_OF_YEAR;
			case ExpressionParserConstants.DATE_UNIT_DAY:
				return Calendar.DAY_OF_MONTH;

			case ExpressionParserConstants.DATE_UNIT_HOUR:
				return Calendar.HOUR_OF_DAY;
			case ExpressionParserConstants.DATE_UNIT_MINUTE:
				return Calendar.MINUTE;
			case ExpressionParserConstants.DATE_UNIT_SECOND:
				return Calendar.SECOND;
			case ExpressionParserConstants.DATE_UNIT_MILLISECOND:
				return Calendar.MILLISECOND;
			default:
				throw new FunctionInputException("expression_parser.function_wrong_type_at", getFunctionName(),
						"unit constant", "third");
		}
	}
}
