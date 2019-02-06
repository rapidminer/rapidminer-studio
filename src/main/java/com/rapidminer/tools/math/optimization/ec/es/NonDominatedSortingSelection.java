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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


/**
 * Performs the non-dominated sorting selection from NSGA-II.
 * 
 * @author Ingo Mierswa
 */
public class NonDominatedSortingSelection implements PopulationOperator {

	/**
	 * The comparator for aggregation individuals using the fitness values of the m-th criterion.
	 */
	private static class CriteriaComparator implements Comparator<Individual>, Serializable {

		private static final long serialVersionUID = -959843514627041473L;

		private int m;

		public CriteriaComparator(int m) {
			this.m = m;
		}

		@Override
		public int compare(Individual i1, Individual i2) {
			double[] f1 = i1.getFitnessValues();
			double[] f2 = i2.getFitnessValues();
			return (-1) * Double.compare(f1[m], f2[m]);
		}
	}

	/** The comparator for aggregation individuals using the crowding distance. */
	private static class CrowdingComparator implements Comparator<Individual>, Serializable {

		private static final long serialVersionUID = -3430652527261319675L;

		@Override
		public int compare(Individual i1, Individual i2) {
			double cd1 = i1.getCrowdingDistance();
			double cd2 = i2.getCrowdingDistance();
			return (-1) * Double.compare(cd1, cd2);
		}
	}

	/** The desired population size. */
	private int popSize;

	/** Creates a new NSGA-II selection. */
	public NonDominatedSortingSelection(int popSize) {
		this.popSize = popSize;
	}

	/** Performs non-dominated sorting selection from NSGA-II. */
	@Override
	public void operate(Population population) {
		List<List<Individual>> ranks = new ArrayList<List<Individual>>();
		while (population.getNumberOfIndividuals() > 0) {
			List<Individual> rank = getNextRank(population);
			ranks.add(rank);
			Iterator<Individual> i = rank.iterator();
			while (i.hasNext()) {
				population.remove(i.next());
			}
		}
		population.clear();

		int index = 0;
		while ((index < ranks.size()) && ((population.getNumberOfIndividuals() + ranks.get(index).size()) <= popSize)) {
			population.addAll(ranks.get(index));
			index++;
		}

		if (population.getNumberOfIndividuals() < popSize) {
			List<Individual> rank = ranks.get(index);
			sortByCrowdingDistance(rank);
			while (population.getNumberOfIndividuals() < popSize) {
				population.add(rank.remove(0));
			}
		}
	}

	/**
	 * Sorts the given rank by crowding distance (i.e. the &quot;neighborhoodship&quot; in the
	 * fitness space).
	 */
	private void sortByCrowdingDistance(List<Individual> rank) {
		Iterator<Individual> f = rank.iterator();
		int numberOfCriteria = 0;
		while (f.hasNext()) {
			Individual current = f.next();
			current.setCrowdingDistance(0.0d);
			numberOfCriteria = Math.max(numberOfCriteria, current.getFitnessValues().length);
		}

		for (int m = 0; m < numberOfCriteria; m++) {
			Comparator<Individual> comparator = new CriteriaComparator(m);
			Collections.sort(rank, comparator);
			rank.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
			rank.get(rank.size() - 1).setCrowdingDistance(Double.POSITIVE_INFINITY);

			for (int i = 1; i < (rank.size() - 1); i++) {
				Individual current = rank.get(i);
				double currentCrowdingDistance = current.getCrowdingDistance();
				Individual afterI = rank.get(i + 1);
				Individual beforeI = rank.get(i - 1);
				double afterPerformance = afterI.getFitnessValues()[m];
				double beforePerformance = beforeI.getFitnessValues()[m];
				current.setCrowdingDistance(currentCrowdingDistance + Math.abs(afterPerformance - beforePerformance));
			}
		}

		Collections.sort(rank, new CrowdingComparator());
	}

	/** Returns a list of non-dominated individuals. */
	private List<Individual> getNextRank(Population population) {
		List<Individual> rank = new ArrayList<Individual>();
		for (int i = 0; i < population.getNumberOfIndividuals(); i++) {
			Individual current = population.get(i);
			rank.add(current);
			boolean delete = false;
			for (int j = rank.size() - 2; j >= 0; j--) {
				Individual ranked = rank.get(j);
				if (isDominated(ranked, current)) {
					rank.remove(ranked);
				}
				if (isDominated(current, ranked)) {
					delete = true;
					// break;
				}
			}
			if (delete) {
				rank.remove(current);
			}
		}
		return rank;
	}

	/**
	 * Returns true if the second performance vector is better in all fitness criteria than the
	 * first one (remember: the criteria should be maximized).
	 */
	public static boolean isDominated(Individual i1, Individual i2) {
		double[] f1 = i1.getFitnessValues();
		double[] f2 = i2.getFitnessValues();
		double[][] performances = new double[f1.length][2];
		for (int p = 0; p < performances.length; p++) {
			performances[p][0] = f1[p];
			performances[p][1] = f2[p];
		}
		boolean dominated = true;
		for (int p = 0; p < performances.length; p++) {
			dominated &= (performances[p][1] >= performances[p][0]);
		}
		boolean oneActuallyBetter = false;
		for (int p = 0; p < performances.length; p++) {
			oneActuallyBetter |= (performances[p][1] > performances[p][0]);
		}
		dominated &= oneActuallyBetter;
		return dominated;
	}
}
