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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Performs a usual GA crossover on integer arrays. Supports one-point and uniform crossover.
 * 
 * @author Ingo Mierswa Exp $
 */
public class AggregationCrossover {

	/** The names for the crossover types. */
	public static final String[] CROSSOVER_TYPES = { "one_point", "uniform" };

	/** Indicates a one-point crossover type. */
	public static final int CROSSOVER_ONE_POINT = 0;

	/** Indicates a uniform crossover type. */
	public static final int CROSSOVER_UNIFORM = 1;

	/** The crossover type. */
	private int crossoverType = CROSSOVER_UNIFORM;

	/** The crossover probability. */
	private double crossoverProb = 0.9;

	private Random random;

	/** Creates a new aggregation crossover operator. */
	public AggregationCrossover(int type, double probability, Random random) {
		this.crossoverType = type;
		this.crossoverProb = probability;
		this.random = random;
	}

	/** Checks if at least one feature is selected. */
	private boolean isValid(int[] individual) {
		for (int i = 0; i < individual.length; i++) {
			if (individual[i] >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Randomly selects parents from the population and performs crossover. The parents are kept.
	 */
	public void crossover(List<AggregationIndividual> population) {
		List<AggregationIndividual> children = new ArrayList<AggregationIndividual>();
		for (int i = 0; i < population.size(); i++) {
			if (random.nextDouble() < crossoverProb) {
				int[] parent1 = population.get(random.nextInt(population.size())).getIndividual();
				int[] parent2 = population.get(random.nextInt(population.size())).getIndividual();
				int[] child1 = new int[parent1.length];
				for (int j = 0; j < child1.length; j++) {
					child1[j] = parent1[j];
				}
				int[] child2 = new int[parent2.length];
				for (int j = 0; j < child2.length; j++) {
					child2[j] = parent2[j];
				}
				crossover(child1, child2);
				if (isValid(child1)) {
					children.add(new AggregationIndividual(child1));
				}
				if (isValid(child2)) {
					children.add(new AggregationIndividual(child2));
				}
			}
		}
		population.addAll(children);
	}

	/**
	 * Changes the individual. Make clones if original individuals should be kept.
	 */
	private void crossover(int[] individual1, int[] individual2) {
		switch (crossoverType) {
			case CROSSOVER_ONE_POINT:
				int n = 1 + random.nextInt(individual1.length - 1);
				for (int i = n; i < individual1.length; i++) {
					int dummy = individual1[i];
					individual1[i] = individual2[i];
					individual2[i] = dummy;
				}
				break;
			case CROSSOVER_UNIFORM:
				for (int i = 0; i < individual1.length; i++) {
					if (random.nextBoolean()) {
						int dummy = individual1[i];
						individual1[i] = individual2[i];
						individual2[i] = dummy;
					}
				}
				break;
			default:
				break;
		}
	}
}
