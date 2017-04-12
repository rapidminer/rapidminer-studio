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
 * The label is att1*att1*att2 + att2*att3 + max(att1,att2) - exp(att3).
 * 
 * @author Ingo Mierswa
 */
public class ComplicatedFunction extends RegressionFunction {

	@Override
	public double calculate(double[] att) throws FunctionException {
		if (att.length < 3) {
			throw new FunctionException("Complicated function", "needs at least 3 attributes!");
		}
		return (att[0] * att[0] * att[1] + att[1] * att[2] + Math.max(att[0], att[1]) - Math.exp(att[2]));
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 3;
	}
}
