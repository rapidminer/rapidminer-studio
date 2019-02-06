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


/**
 * This PopulationOperator realizes backward elimination, i.e. creates a list of clones of each
 * individual and switches of one attribute in each of the clones.
 * 
 * @author Simon Fischer, Ingo Mierswa Exp $
 */
public class BackwardElimination extends IndividualOperator {

	@Override
	public List<Individual> operate(Individual individual) {
		double[] weights = individual.getWeights();

		List<Individual> l = new LinkedList<Individual>();
		for (int i = 0; i < weights.length; i++) {
			if (weights[i] > 0) {
				double[] newWeights = new double[weights.length];
				System.arraycopy(weights, 0, newWeights, 0, weights.length);
				newWeights[i] = 0;
				Individual newIndividual = new Individual(newWeights);
				if (newIndividual.getNumberOfUsedAttributes() > 0) {
					l.add(newIndividual);
				}
			}
		}
		return l;
	}
}
