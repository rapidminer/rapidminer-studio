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
import java.util.concurrent.Callable;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.FunctionDescription;


/**
 * A {@link com.rapidminer.tools.expression.Function Function} for setting a value of a given date.
 *
 * @author David Arnu, Jan Czogalla
 *
 */
public class DateSet extends AbstractDateLongManipulationFunction {

	public DateSet() {
		super("date.date_set", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, Ontology.DATE_TIME);
	}

	/** Sets the given {@code value} with the specified unit in the calendar; For {@link Calendar#YEAR}, caps the value. */
	@Override
	void integerManipulation(Callable<Void> stopChecker, Calendar cal, int unit, int value) {
		if (unit == Calendar.YEAR) {
			if (value < -cal.getLeastMaximum(unit)) {
				value = -cal.getLeastMaximum(unit);
			} else if (value >= cal.getMaximum(unit)) {
				value = cal.getMaximum(unit) - 1;
			}
		}
		cal.set(unit, value);
	}

	/**
	 * For {@link Calendar#YEAR} as {@code dateUnit}, calls {@link #integerManipulation(Callable, Calendar, int, int)} with
	 * {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE}, depending on{@code sign}. For all other fields,
	 * uses modulo calculations to find the appropriate value to set.
	 */
	@Override
	void longManipulation(Callable<Void> stopChecker, Calendar cal, long dateValue, int sign, int dateUnit) {
		if (dateUnit == Calendar.YEAR) {
			integerManipulation(stopChecker, cal, dateUnit, sign == 1 ? Integer.MAX_VALUE : Integer.MIN_VALUE);
			return;
		}
		integerManipulation(stopChecker, cal, dateUnit, 0);
		new DateAdd().longManipulation(stopChecker, cal, dateValue, sign, dateUnit);
	}
}
