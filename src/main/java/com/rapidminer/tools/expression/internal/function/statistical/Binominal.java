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
package com.rapidminer.tools.expression.internal.function.statistical;

import org.apache.commons.math3.util.CombinatoricsUtils;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.Abstract2DoubleInputFunction;


/**
 *
 * A {@link Function} for binominal coefficents.
 *
 * @author David Arnu
 *
 */
public class Binominal extends Abstract2DoubleInputFunction {

	/**
	 * Constructs a binominal function.
	 */
	public Binominal() {
		super("statistical.binom", 2, Ontology.INTEGER);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType left = inputTypes[0];
		ExpressionType right = inputTypes[1];

		if (left == ExpressionType.INTEGER && right == ExpressionType.INTEGER) {
			return ExpressionType.INTEGER;
		} else {
			throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "integer");
		}
	}

	@Override
	protected double compute(double value1, double value2) {

		// special case for handling missing values
		if (Double.isNaN(value1) || Double.isNaN(value2)) {
			return Double.NaN;
		}

		if (value1 < 0 || value2 < 0) {
			throw new FunctionInputException("expression_parser.function_non_negative", getFunctionName());
		}
		// This is the common definition for the case for k > n.
		if (value2 > value1) {
			return 0;
		} else {
			return CombinatoricsUtils.binomialCoefficientDouble((int) value1, (int) value2);
		}
	}

}
