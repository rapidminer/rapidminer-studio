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
package com.rapidminer.operator.features.selection;

import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.IndividualOperator;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * Inverts the used bit for every feature of every example set with a given fixed probability.
 * 
 * @author Simon Fischer, Ingo Mierswa Exp $
 */
public class SelectionMutation extends IndividualOperator {

	private double probability;

	private Random random;

	private int minNumber;

	private int maxNumber;

	private int exactNumber;

	public SelectionMutation(double probability, Random random, int minNumber, int maxNumber, int exactNumber) {
		this.probability = probability;
		this.random = random;
		this.minNumber = minNumber;
		this.maxNumber = maxNumber;
		this.exactNumber = exactNumber;
	}

	@Override
	public List<Individual> operate(Individual individual) {
		List<Individual> l = new LinkedList<Individual>();
		double[] weights = individual.getWeightsClone();

		double prob = probability < 0 ? 1.0d / weights.length : probability;
		for (int i = 0; i < weights.length; i++) {
			if (random.nextDouble() < prob) {
				if (weights[i] > 0) {
					weights[i] = 0;
				} else {
					weights[i] = 1;
				}
			}
		}

		Individual newIndividual = new Individual(weights);
		int numberOfFeatures = newIndividual.getNumberOfUsedAttributes();
		if (numberOfFeatures > 0) {
			if (exactNumber > 0) {
				if (numberOfFeatures == exactNumber) {
					l.add(newIndividual);
				}
			} else {
				if (((maxNumber < 1) || (numberOfFeatures <= maxNumber)) && (numberOfFeatures >= minNumber)) {
					l.add(newIndividual);
				}
			}
		}

		// add also original ES
		l.add(individual);
		return l;
	}
}
