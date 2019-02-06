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
package com.rapidminer.operator.generator;

/**
 * The sinc function on R^n for n >= 1
 * 
 * The Label is f(x) = sin(x) / ||x||, if ||x|| != 0, and 0 else.
 * 
 * @author Piotr Kasprzak
 */
public class SincFunction extends RegressionFunction {

	/* L2 norm on R^n */
	public double norm_l2(double[] vector) {
		double result = 0;
		for (int i = 0; i < vector.length; i++) {
			result += vector[i] * vector[i];
		}
		return Math.sqrt(result);
	}

	@Override
	public double calculate(double[] att) {

		double norm = norm_l2(att);
		double result;

		if (norm <= Double.MIN_VALUE) {
			// Treat as 0
			result = 0;
		} else {
			result = Math.sin(norm) / norm;
		}

		return result;
	}
}
