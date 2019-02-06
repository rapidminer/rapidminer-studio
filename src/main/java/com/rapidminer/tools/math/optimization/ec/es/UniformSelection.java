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
 * Selects a given fixed number of individuals by uniformly sampling from the current population
 * until the desired population size is reached.
 * 
 * @author Ingo Mierswa
 */
public class UniformSelection implements PopulationOperator {

	private int popSize;

	private boolean keepBest;

	private Random random;

	public UniformSelection(int popSize, boolean keepBest, Random random) {
		this.popSize = popSize;
		this.keepBest = keepBest;
		this.random = random;
	}

	/** The default implementation returns true for every generation. */
	public boolean performOperation(int generation) {
		return true;
	}

	@Override
	public void operate(Population population) {
		List<Individual> newGeneration = new LinkedList<Individual>();

		if (keepBest) {
			newGeneration.add(population.getBestEver());
		}

		while (newGeneration.size() < popSize) {
			newGeneration.add(population.get(random.nextInt(population.getNumberOfIndividuals())));
		}

		population.clear();
		population.addAll(newGeneration);
	}
}
