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

import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;


/**
 * A {@link Function} for calculating the difference between two dates in milliseconds.
 *
 * @author David Arnu
 *
 */
public class DateDiff extends Abstract2DateInputIntegerOutputFunction {

	public DateDiff() {
		super("date.date_diff", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS);
	}

	@Override
	protected double compute(Date left, Date right, String valueLocale, String valueTimezone) {
		if (left == null || right == null) {
			return Double.NaN;
		} else {

			return right.getTime() - left.getTime();
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		// locale and time zone arguments just accepted out of compatibility reasons, but are no
		// longer documented
		if (inputTypes.length != 2 && inputTypes.length != 4) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), "2",
					inputTypes.length);
		}
		ExpressionType firstType = inputTypes[0];
		ExpressionType secondType = inputTypes[1];
		if (firstType != ExpressionType.DATE || secondType != ExpressionType.DATE) {
			throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "date");
		}
		if (inputTypes.length == 4) {
			if (inputTypes[2] != ExpressionType.STRING) {
				throw new FunctionInputException("expression_parser.function_date_diff_depricated", getFunctionName());
			}
			if (inputTypes[3] != ExpressionType.STRING) {
				throw new FunctionInputException("expression_parser.function_date_diff_depricated", getFunctionName());
			}
		}
		return ExpressionType.INTEGER;

	}

}
