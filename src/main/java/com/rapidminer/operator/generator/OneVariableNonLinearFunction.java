/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
 * The label is 3*att1*att1*att1 - att1*att1 + 1000 / abs(att1) + 2000 * abs(att1).
 * 
 * @author Ingo Mierswa
 */
public class OneVariableNonLinearFunction extends RegressionFunction {

	@Override
	public double calculate(double[] att) throws FunctionException {
		if (att.length != 1) {
			throw new FunctionException("One variable non linear", "needs one attribute!");
		}
		return 3.0d * att[0] * att[0] * att[0] - att[0] * att[0] + 1000.0d / Math.abs(att[0]) + 2000.0d * Math.abs(att[0]);
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
