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

import com.rapidminer.tools.RandomGenerator;


/**
 * The label is randomly generated.
 * 
 * @author Ingo Mierswa
 */
public class RandomFunction extends RegressionFunction {

	private RandomGenerator random;

	@Override
	public void init(RandomGenerator random) {
		this.random = random;
	}

	@Override
	public double calculate(double[] args) {
		return random.nextDouble();
	}
}
