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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * Crossover operator for the used bitlists of example sets. An example set is selected with a given
 * fixed propability and a mating partner is determined randomly. Crossover can be either one point,
 * uniform or shuffled. Please note that shuffled crossover first uniformly determines the number of
 * attributes which should be swapped. Therefore uniform and shuffle crossover are not equivalent. <br>
 * 
 * Only useful if all example sets have the same (number of) attributes.
 * 
 * @author Simon Fischer, Ingo Mierswa Exp $
 */
public class SelectionCrossover implements PopulationOperator {

	public static final String[] CROSSOVER_TYPES = { "one_point", "uniform", "shuffle" };

	public static final int ONE_POINT = 0;

	public static final int UNIFORM = 1;

	public static final int SHUFFLE = 2;

	private int type;

	private double prob;

	private Random random;

	private int minNumber;

	private int maxNumber;

	private int exactNumber;

	public SelectionCrossover(int type, double prob, Random random, int minNumber, int maxNumber, int exactNumber) {
		this.prob = prob;
		this.type = type;
		this.random = random;
		this.minNumber = minNumber;
		this.maxNumber = maxNumber;
		this.exactNumber = exactNumber;
	}

	/** The default implementation returns true for every generation. */
	@Override
	public boolean performOperation(int generation) {
		return true;
	}

	public int getType() {
		return type;
	}

	public void crossover(double[] weights1, double[] weights2) {
		switch (type) {
			case ONE_POINT:
				int n = 1 + random.nextInt(weights1.length - 1);
				for (int index = n; index < weights1.length; index++) {
					double dummy = weights1[index];
					weights1[index] = weights2[index];
					weights2[index] = dummy;
				}
				break;
			case UNIFORM:
				boolean[] swap = new boolean[weights1.length];
				for (int i = 0; i < swap.length; i++) {
					swap[i] = random.nextBoolean();
					;
				}
				swapAttributes(weights1, weights2, swap);
				break;
			case SHUFFLE:
				swap = new boolean[weights1.length];
				List<Integer> indices = new ArrayList<Integer>();
				for (int i = 0; i < swap.length; i++) {
					indices.add(i);
				}
				if (indices.size() > 0) {
					int toSwap = random.nextInt(indices.size() - 1) + 1;
					for (int i = 0; i < toSwap; i++) {
						swap[indices.remove(random.nextInt(indices.size()))] = true;
					}
				}
				swapAttributes(weights1, weights2, swap);
				break;
			default:
				break;
		}
	}

	private void swapAttributes(double[] weights1, double[] weights2, boolean[] swap) {
		for (int index = 0; index < weights1.length; index++) {
			if (swap[index]) {
				double dummy = weights1[index];
				weights1[index] = weights2[index];
				weights2[index] = dummy;
			}
		}
	}

	@Override
	public void operate(Population population) {
		if (population.getNumberOfIndividuals() < 2) {
			return;
		}

		LinkedList<double[]> matingPool = new LinkedList<double[]>();
		for (int i = 0; i < population.getNumberOfIndividuals(); i++) {
			matingPool.add(population.get(i).getWeightsClone());
		}

		List<Individual> l = new LinkedList<Individual>();

		while (matingPool.size() > 1) {
			double[] p1 = matingPool.remove(random.nextInt(matingPool.size()));
			double[] p2 = matingPool.remove(random.nextInt(matingPool.size()));

			if (random.nextDouble() < prob) {
				crossover(p1, p2);

				Individual newIndividual1 = new Individual(p1);
				int numberOfFeatures = newIndividual1.getNumberOfUsedAttributes();
				if (numberOfFeatures > 0) {
					if (exactNumber > 0) {
						if (numberOfFeatures == exactNumber) {
							l.add(newIndividual1);
						}
					} else {
						if (((maxNumber < 1) || (numberOfFeatures <= maxNumber)) && (numberOfFeatures >= minNumber)) {
							l.add(newIndividual1);
						}
					}
				}

				Individual newIndividual2 = new Individual(p2);
				numberOfFeatures = newIndividual2.getNumberOfUsedAttributes();
				if (numberOfFeatures > 0) {
					if (exactNumber > 0) {
						if (numberOfFeatures == exactNumber) {
							l.add(newIndividual2);
						}
					} else {
						if (((maxNumber < 1) || (numberOfFeatures <= maxNumber)) && (numberOfFeatures >= minNumber)) {
							l.add(newIndividual2);
						}
					}
				}
			}
		}

		population.addAllIndividuals(l);
	}
}
