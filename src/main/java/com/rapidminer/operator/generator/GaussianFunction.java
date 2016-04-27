/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;


/**
 * Generates a gaussian distribution for all attributes.
 * 
 * @author Ingo Mierswa
 */
public class GaussianFunction extends RegressionFunction {

	private double bound = 10.0d;

	/** Since circles are used the upper and lower bounds must be the same. */
	@Override
	public void setLowerArgumentBound(double lower) {
		this.bound = Math.max(this.bound, Math.abs(lower));
	}

	@Override
	public void setUpperArgumentBound(double upper) {
		this.bound = Math.max(this.bound, Math.abs(upper));
	}

	@Override
	public Attribute getLabel() {
		return AttributeFactory.createAttribute("label", Ontology.REAL);
	}

	@Override
	public double calculate(double[] att) throws FunctionException {
		return 0.0d;
	}

	@Override
	public double[] createArguments(int number, RandomGenerator random) {
		double[] args = new double[number];
		for (int i = 0; i < args.length; i++) {
			args[i] = random.nextGaussian() * bound;
		}
		return args;
	}
}
