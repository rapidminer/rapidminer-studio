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
public class DateAdd extends AbstractDateLongManipulationFunction {

	public DateAdd() {
		super("date.date_add", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, Ontology.DATE_TIME);
	}

	/** Adds the given {@code value} with the specified unit to the calendar */
	@Override
	void integerManipulation(Callable<Void> stopChecker, Calendar cal, int unit, int value) {
		cal.add(unit, value);
	}

	/**
	 * Call {@link AbstractDateLongManipulationFunction#integerManipulation(Callable, Calendar, int, int)} with
	 * {@link #MAX_DATE_VALUE} until there is only an integer rest and finally add that.
	 */
	@Override
	void longManipulation(Callable<Void> stopChecker, Calendar cal, long dateValue, int sign, int dateUnit) {
		try {
			while (dateValue >= MAX_DATE_VALUE) {
				integerManipulation(stopChecker, cal, dateUnit, MAX_DATE_VALUE * sign);
				dateValue -= MAX_DATE_VALUE;
				stopChecker.call();
			}
		} catch (Exception e) {
			// stop checker forced calculation to stop
			return;
		}
		integerManipulation(stopChecker, cal, dateUnit, (int) (sign * dateValue));
	}
}
