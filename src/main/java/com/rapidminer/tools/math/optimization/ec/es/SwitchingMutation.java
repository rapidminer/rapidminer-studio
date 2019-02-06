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
 * Checks for each value if it should mutated. Sets a non-min value to min and a zero value to max.
 * 
 * @author Ingo Mierswa Exp $
 */
public class SwitchingMutation implements Mutation {

	private double prob;

	private double[] min, max;

	private OptimizationValueType[] valueTypes;

	private Random random;

	public SwitchingMutation(double prob, double[] min, double[] max, OptimizationValueType[] valueTypes, Random random) {
		this.prob = prob;
		this.min = min;
		this.max = max;
		this.valueTypes = valueTypes;
		this.random = random;
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
			boolean changed = false;
			for (int j = 0; j < values.length; j++) {
				if (random.nextDouble() < prob) {
					changed = true;
					if (values[j] != min[j]) {
						values[j] = min[j];
					} else {
						values[j] = max[j];
					}
					if (valueTypes[j].equals(OptimizationValueType.VALUE_TYPE_INT)) {
						values[j] = (int) Math.round(values[j]);
					}
				}
			}
			if (changed) {
				clone.setValues(values);
				newIndividuals.add(clone);
			}
		}
		population.addAll(newIndividuals);
	}
}
