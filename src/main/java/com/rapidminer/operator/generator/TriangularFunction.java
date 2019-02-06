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
 * The label is att1 - (int)att1.
 * 
 * @author Ingo Mierswa
 */
public class TriangularFunction extends RegressionFunction {

	@Override
	public double calculate(double[] args) throws FunctionException {
		if (args.length != 1) {
			throw new FunctionException("Triangular function", "needs 1 attributes!");
		}
		// double a = 100.0d;
		// double b = 5.0d;
		// return a * (4.0d * Math.abs(Math.atan(Math.tan((args[0]/ b + 1.0d
		// / 4.0d) * Math.PI)) / Math.PI) - 1.0d);
		return args[0] - (int) args[0];
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 1;
	}

	@Override
	public int getMaxNumberOfAttributes() {
		return 1;
	}
}
