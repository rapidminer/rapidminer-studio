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
import java.util.Random;


/**
 * Changes the weight for all attributes by multiplying them with a gaussian distribution.
 * 
 * @author Ingo Mierswa
 */
public class WeightingMutation extends IndividualOperator {

	private double variance;

	private boolean bounded;

	private Random random;

	private boolean[] isNominal;

	private double nominalMutationProb;

	public WeightingMutation(double variance, boolean bounded, boolean[] isNominal, double nominalMutationProb, Random random) {
		this.variance = variance;
		this.bounded = bounded;
		this.random = random;
		this.isNominal = isNominal;
		this.nominalMutationProb = nominalMutationProb;
	}

	public void setVariance(double variance) {
		this.variance = variance;
	}

	public double getVariance() {
		return variance;
	}

	@Override
	public List<Individual> operate(Individual individual) {
		double[] weights = individual.getWeightsClone();
		List<Individual> l = new LinkedList<Individual>();
		for (int i = 0; i < weights.length; i++) {
			if (!isNominal[i]) {
				if (random.nextDouble() < nominalMutationProb) {
					if (weights[i] > 0) {
						weights[i] = 0;
					} else {
						weights[i] = 1;
					}
				}
			} else {
				double weight = weights[i] + random.nextGaussian() * variance;
				if ((!bounded) || ((weight >= 0) && (weight <= 1))) {
					weights[i] = weight;
				}
			}
		}
		Individual newIndividual = new Individual(weights);
		if (newIndividual.getNumberOfUsedAttributes() > 0) {
			l.add(newIndividual);
		}
		return l;
	}
}
