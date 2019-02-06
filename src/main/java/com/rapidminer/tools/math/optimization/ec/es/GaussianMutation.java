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
package com.rapidminer.tools.math.optimization.ec.es;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * Changes the values by adding a gaussian distribution multiplied with the current variance. Clips
 * the value range to [min,max].
 * 
 * @author Ingo Mierswa
 */
public class GaussianMutation implements Mutation {

	private double[] sigma;

	private double[] min, max;

	private OptimizationValueType[] valueTypes;

	private Random random;

	public GaussianMutation(double[] sigma, double[] min, double[] max, OptimizationValueType[] valueTypes, Random random) {
		this.sigma = sigma;
		this.min = min;
		this.max = max;
		this.valueTypes = valueTypes;
		this.random = random;
	}

	public void setSigma(double[] sigma) {
		this.sigma = sigma;
	}

	public double[] getSigma() {
		return this.sigma;
	}

	@Override
	public void setValueType(int index, OptimizationValueType type) {
		this.valueTypes[index] = type;
	}

	@Override
	public void operate(Population population) {
		List<Individual> newIndividuals = new LinkedList<Individual>();
		for (int i = 0; i < population.getNumberOfIndividuals(); i++) {
			Individual clone = (Individual) population.get(i).clone();
			double[] values = clone.getValues();
			for (int j = 0; j < values.length; j++) {
				if (valueTypes[j].equals(OptimizationValueType.VALUE_TYPE_INT)) {
					values[j] += random.nextGaussian() * sigma[j];
					values[j] = (int) Math.round(values[j]);
				} else if (valueTypes[j].equals(OptimizationValueType.VALUE_TYPE_BOUNDS)) {
					if (random.nextDouble() < 1.0d / values.length) {
						if (values[j] >= (max[j] - min[j]) / 2.0d) {
							values[j] = min[j];
						} else {
							values[j] = max[j];
						}
					}
				} else {
					values[j] += random.nextGaussian() * sigma[j];
				}

				if (values[j] < min[j]) {
					values[j] = min[j];
				}
				if (values[j] > max[j]) {
					values[j] = max[j];
				}
			}

			clone.setValues(values);
			newIndividuals.add(clone);
		}
		population.addAll(newIndividuals);
	}
}
