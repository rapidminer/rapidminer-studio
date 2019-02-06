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
package com.rapidminer.tools.expression.internal.function.trigonometric;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.Abstract2DoubleInputFunction;


/**
 *
 * A {@link Function} computing the trigonometric arc tangent of two values.
 *
 * @author Denis Schernov
 *
 */
public class ArcTangent2 extends Abstract2DoubleInputFunction {

	public ArcTangent2() {
		super("trigonometrical.atan2", 2, Ontology.NUMERICAL);
	}

	@Override
	protected double compute(double value1, double value2) {
		return Double.isNaN(value1) || Double.isNaN(value2) ? Double.NaN : Math.atan2(value1, value2);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType left = inputTypes[0];
		ExpressionType right = inputTypes[1];
		if ((left == ExpressionType.INTEGER || left == ExpressionType.DOUBLE)
				&& (right == ExpressionType.INTEGER || right == ExpressionType.DOUBLE)) {
			return ExpressionType.DOUBLE;
		} else {
			throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "numerical");
		}
	}
}
