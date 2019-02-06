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

import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * Performs a tournament selection on the given population list.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class AggregationTournamentSelection implements AggregationSelection {

	private int popSize = 10;

	private double tournamentFraction = 0.25;

	private Random random;

	public AggregationTournamentSelection(int popSize, double tournamentFraction, Random random) {
		this.popSize = popSize;
		this.tournamentFraction = tournamentFraction;
		this.random = random;
	}

	@Override
	public void performSelection(List<AggregationIndividual> population) {
		List<AggregationIndividual> newGeneration = new LinkedList<AggregationIndividual>();
		int tournamentSize = Math.max((int) Math.round(population.size() * tournamentFraction), 1);

		while (newGeneration.size() < popSize) {
			AggregationIndividual winner = null;
			for (int k = 0; k < tournamentSize; k++) {
				AggregationIndividual current = population.get(random.nextInt(population.size()));
				if ((winner == null)
						|| (current.getPerformance().getMainCriterion().getFitness() > (winner.getPerformance()
								.getMainCriterion().getFitness()))) {
					winner = current;
				}
			}
			newGeneration.add(winner);
		}

		population.clear();
		population.addAll(newGeneration);
	}
}
