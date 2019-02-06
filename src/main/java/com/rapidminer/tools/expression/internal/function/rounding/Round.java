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
package com.rapidminer.tools.expression.internal.function.rounding;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionInputException;


/**
 * A {@link Function} for rounding numbers.
 *
 * @author David Arnu
 *
 */
public class Round extends Abstract1or2DoubleInputFunction {

	public Round() {
		super("rounding.round", Ontology.NUMERICAL);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType firstType = inputTypes[0];
		if (firstType == ExpressionType.INTEGER || firstType == ExpressionType.DOUBLE && inputTypes.length == 1) {
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
		// we keep this way of calculating the rounding to ensure the compatibility with the
		// previous parser regarding doubles as number of digits

		if (Double.isNaN(value2)) {
			return compute(value1);
		}
		if (value2 == Double.NEGATIVE_INFINITY) {
			return Double.NaN;
		}
		// this is a special case in the old parser and we keep it out of compatibility reasons.
		if (value2 == Double.POSITIVE_INFINITY) {
			return 0;
		}
		if (Double.isNaN(value1) || value1 == Double.POSITIVE_INFINITY || value1 == Double.NEGATIVE_INFINITY) {
			return value1;
		}

		int dp = (int) value2;
		double mul = Math.pow(10, dp);
		return Math.round(value1 * mul) / mul;
	}

	@Override
	protected double compute(double value) {

		// don't change missing values. Math.round() would return 0
		if (Double.isNaN(value)) {
			return value;
		}
		return Math.round(value);
	}

}
