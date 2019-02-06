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
 * The label is sin(att1*att2) + sin(att1+att2).
 * 
 * @author Ingo Mierswa
 */
public class SinusFunction extends RegressionFunction {

	@Override
	public double calculate(double[] att) throws FunctionException {
		if (att.length != 2) {
			throw new FunctionException("Sinus function", "needs 2 attributes!");
		}
		return (Math.sin(att[0] * att[1]) + Math.sin(att[0] + att[1]));
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 2;
	}

	@Override
	public int getMaxNumberOfAttributes() {
		return 2;
	}
}
