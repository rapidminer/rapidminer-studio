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
import java.util.Random;


/**
 * Performs tournaments with k participants. The winner of each tournament is added to the next
 * population. This will be repeated until the desired population size is reached. The tournament
 * size k represents the selection pressure. For small sizes (like 2) relatively bad individuals
 * have a good chance to survive. If k reaches the population size, only the best individual will
 * survive.
 * 
 * @author Ingo Mierswa Exp $
 */
public class TournamentSelection implements PopulationOperator {

	private int popSize;

	private double tournamentFraction;

	private double delta = 0.0d;

	private boolean keepBest = false;

	private Random random;

	public TournamentSelection(int popSize, double tournamentFraction, int maxGenerations, boolean dynamic,
			boolean keepBest, Random random) {
		this.popSize = popSize;
		this.keepBest = keepBest;
		this.tournamentFraction = tournamentFraction;
		if (dynamic) {
			delta = (1.0d - this.tournamentFraction) / (maxGenerations + 1);
		}
		this.random = random;
	}

	@Override
	public boolean performOperation(int generation) {
		return true;
	}

	@Override
	public void operate(Population population) {
		List<Individual> newGeneration = new LinkedList<Individual>();
		int tournamentSize = Math.max((int) Math.round(population.getNumberOfIndividuals() * tournamentFraction), 1);

		if (keepBest) {
			newGeneration.add(population.getBestIndividualEver());
		}

		while (newGeneration.size() < popSize) {
			Individual winner = null;
			for (int k = 0; k < tournamentSize; k++) {
				Individual current = population.get(random.nextInt(population.getNumberOfIndividuals()));
				if ((winner == null)
						|| (current.getPerformance().getMainCriterion().getFitness() > (winner.getPerformance()
								.getMainCriterion().getFitness()))) {
					winner = current;
				}
			}
			newGeneration.add(winner);
		}

		population.clear();
		population.addAllIndividuals(newGeneration);
		this.tournamentFraction += delta;
	}
}
