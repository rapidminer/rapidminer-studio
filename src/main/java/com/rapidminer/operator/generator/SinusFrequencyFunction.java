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
 * The label is 10 * sin(3 * att1) + 12 * sin(7 * att1) + 11 * sin(5 * att2) + 9 * sin(10 * att2) +
 * 10 * sin(8 * (att1 + att2)).
 * 
 * @author Ingo Mierswa
 */
public class SinusFrequencyFunction extends RegressionFunction {

	@Override
	public double calculate(double[] att) throws FunctionException {
		if (att.length < 2) {
			throw new FunctionException("Sinus frequency function", "needs at least 2 attributes!");
		}
		return 10 * Math.sin(3 * att[0]) + 12 * Math.sin(7 * att[0]) + 11 * Math.sin(5 * att[1]) + 9 * Math.sin(10 * att[1])
				+ 10 * Math.sin(8 * (att[0] + att[1]));
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 2;
	}
}
