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
 * A {@link Function} to rint numbers.
 *
 * @author Thilo Kamradt
 *
 */
public class Rint extends Abstract1or2DoubleInputFunction {

	public Rint() {
		super("rounding.rint", Ontology.NUMERICAL);
	}

	@Override
	protected double compute(double value) {
		return Math.rint(value);
	}

	@Override
	protected double compute(double value1, double value2) {
		if (Double.isNaN(value2)) {
			return compute(value1);
		}
		if (Double.isNaN(value1) || value2 == Double.POSITIVE_INFINITY || value2 == Double.NEGATIVE_INFINITY) {
			return Double.NaN;
		}

		int factor = (int) Math.pow(10, (int) value2);
		return Math.rint(value1 * factor) / factor;
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes[0] == ExpressionType.INTEGER || inputTypes[0] == ExpressionType.DOUBLE && inputTypes.length == 1) {
			return ExpressionType.INTEGER;
		} else if (inputTypes[0] == ExpressionType.DOUBLE && inputTypes.length == 2
				&& (inputTypes[1] == ExpressionType.INTEGER || inputTypes[1] == ExpressionType.DOUBLE)) {
			return ExpressionType.DOUBLE;
		} else {
			throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "numerical");
		}
	}
}
