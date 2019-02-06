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
 * Similar to a the roulette wheel selection the fitness values of all individuals build a partition
 * of the 360 degrees of a wheel. The wheel is turned only once and the individuals are selected
 * based on equidistant marks on the wheel. Optionally the best individual is also kept.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class StochasticUniversalSampling implements PopulationOperator {

	private int popSize;

	private boolean keepBest;

	private Random random;

	public StochasticUniversalSampling(int popSize, boolean keepBest, Random random) {
		this.popSize = popSize;
		this.keepBest = keepBest;
		this.random = random;
	}

	/** The default implementation returns true for every generation. */
	public boolean performOperation(int generation) {
		return true;
	}

	/**
	 * Subclasses may override this method and recalculate the fitness based on the given one, e.g.
	 * Boltzmann selection or scaled selection. The default implementation simply returns the given
	 * fitness.
	 */
	public double filterFitness(double fitness) {
		return fitness;
	}

	@Override
	public void operate(Population population) {
		List<Individual> newGeneration = new LinkedList<Individual>();

		if (keepBest) {
			newGeneration.add(population.getBestEver());
		}

		int numberOfMarks = popSize - newGeneration.size();
		double distance = 1.0d / numberOfMarks;
		double r = random.nextDouble() / numberOfMarks;
		for (int i = 0; i < numberOfMarks; i++) {
			double f = 0;
			int j = 0;
			Individual individual = null;
			do {
				individual = population.get(j++);
				f += filterFitness(individual.getFitness().getMainCriterion().getFitness());
			} while (f < r);
			newGeneration.add(individual);
			r += distance;
		}

		population.clear();
		population.addAll(newGeneration);
	}
}
