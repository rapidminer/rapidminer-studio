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
package com.rapidminer.operator.features.weighting;

import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.IndividualOperator;

import java.util.LinkedList;
import java.util.List;


/**
 * This PopulationOperator realizes a simple weighting, i.e. creates a list of clones of each
 * individual and weights one attribute in each of the clones with some different weights.
 * 
 * @author Ingo Mierswa
 */
public class SimpleWeighting extends IndividualOperator {

	/**
	 * If a weight is equal to this compare weight, all weights are used for building a new
	 * individual.
	 */
	private double compareWeight = 0.0d;

	/** These weights are used for building new individuals. */
	private double[] weights = null;

	/** Creates a simple weighting. */
	public SimpleWeighting(double compareWeight, double[] weights) {
		this.compareWeight = compareWeight;
		this.weights = weights;
	}

	@Override
	public List<Individual> operate(Individual individual) {
		double[] orginal = individual.getWeightsClone();
		List<Individual> l = new LinkedList<Individual>();
		for (int i = 0; i < orginal.length; i++) {
			if (orginal[i] == compareWeight) {
				for (int w = 0; w < weights.length; w++) {
					double[] clone = individual.getWeightsClone();
					clone[i] = weights[w];
					l.add(new Individual(clone));
				}
			}
		}
		return l;
	}
}
