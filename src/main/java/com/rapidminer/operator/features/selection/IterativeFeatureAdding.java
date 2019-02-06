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
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;

import java.util.LinkedList;
import java.util.List;


/**
 * Adds iteratively the next feature according to given attribute name array.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class IterativeFeatureAdding implements PopulationOperator {

	private int[] indices;

	private int counter;

	public IterativeFeatureAdding(int[] attributeIndices, int counter) {
		this.indices = attributeIndices;
		this.counter = counter;
	}

	/** The default implementation returns true for every generation. */
	@Override
	public boolean performOperation(int generation) {
		return true;
	}

	@Override
	public void operate(Population pop) {
		List<Individual> result = new LinkedList<Individual>();
		for (int i = 0; i < pop.getNumberOfIndividuals(); i++) {
			if (counter < indices.length) {
				double[] weights = pop.get(i).getWeightsClone();
				weights[indices[counter]] = 1.0d;
				result.add(new Individual(weights));
			}
		}
		pop.clear();
		pop.addAllIndividuals(result);
		counter++;
	}
}
