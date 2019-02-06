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
package com.rapidminer.operator.features.aggregation;

import com.rapidminer.tools.RandomGenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Performs an aggregation mutation on integer arrays. Each feature value is mutated with
 * probability 1/n. Mutation is done by randomly selecting a new value between -1 and max(values).
 * 
 * @author Ingo Mierswa
 */
public class AggregationMutation {

	private RandomGenerator random;

	public AggregationMutation(RandomGenerator random) {
		this.random = random;
	}

	/** Checks if at least one feature was selected. */
	private boolean isValid(int[] individual) {
		for (int i = 0; i < individual.length; i++) {
			if (individual[i] >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Invokes the method mutate(int[]) for each individual. The parents are kept.
	 */
	public void mutate(List<AggregationIndividual> population) {
		List<AggregationIndividual> children = new ArrayList<AggregationIndividual>();
		Iterator<AggregationIndividual> i = population.iterator();
		while (i.hasNext()) {
			AggregationIndividual individual = i.next();
			int[] parent = individual.getIndividual();
			int[] child = new int[parent.length];
			for (int j = 0; j < child.length; j++) {
				child[j] = parent[j];
			}
			mutate(child);
			if (isValid(child)) {
				children.add(new AggregationIndividual(child));
			}
		}
		population.addAll(children);
	}

	/**
	 * Changes the individual (each gene with probability 1 / n). Make clone if original individual
	 * should be kept.
	 */
	private void mutate(int[] individual) {
		double prob = 1.0d / individual.length;
		for (int i = 0; i < individual.length; i++) {
			if (random.nextDouble() < prob) {
				individual[i] = random.nextIntInRange(0, 2);
			}
		}
	}
}
