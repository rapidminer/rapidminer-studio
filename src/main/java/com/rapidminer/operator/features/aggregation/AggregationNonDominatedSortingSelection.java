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

import com.rapidminer.operator.performance.PerformanceVector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


/**
 * Performs the non-dominated sorting selection from NSGA-II.
 * 
 * @author Ingo Mierswa 07:47:16 ingomierswa Exp $
 */
public class AggregationNonDominatedSortingSelection implements AggregationSelection {

	/**
	 * The comparator for aggregation individuals using the fitness values of the m-th criterion.
	 */
	private static class CriteriaComparator implements Comparator<AggregationIndividual>, Serializable {

		private static final long serialVersionUID = 4188525398056332510L;

		private int m;

		public CriteriaComparator(int m) {
			this.m = m;
		}

		@Override
		public int compare(AggregationIndividual i1, AggregationIndividual i2) {
			PerformanceVector pv1 = i1.getPerformance();
			PerformanceVector pv2 = i2.getPerformance();
			return (-1) * Double.compare(pv1.getCriterion(m).getFitness(), pv2.getCriterion(m).getFitness());
		}
	}

	/** The comparator for aggregation individuals using the crowding distance. */
	private static class CrowdingComparator implements Comparator<AggregationIndividual>, Serializable {

		private static final long serialVersionUID = 4712425942757940295L;

		@Override
		public int compare(AggregationIndividual i1, AggregationIndividual i2) {
			double cd1 = i1.getCrowdingDistance();
			double cd2 = i2.getCrowdingDistance();
			return (-1) * Double.compare(cd1, cd2);
		}
	}

	/** The desired population size. */
	private int popSize;

	/** Creates a new NSGA-II selection. */
	public AggregationNonDominatedSortingSelection(int popSize) {
		this.popSize = popSize;
	}

	/** Performs non-dominated sorting selection from NSGA-II. */
	@Override
	public void performSelection(List<AggregationIndividual> population) {
		List<List<AggregationIndividual>> ranks = new ArrayList<List<AggregationIndividual>>();
		while (population.size() > 0) {
			List<AggregationIndividual> rank = getNextRank(population);
			ranks.add(rank);
			Iterator<AggregationIndividual> i = rank.iterator();
			while (i.hasNext()) {
				population.remove(i.next());
			}
		}
		population.clear();

		int index = 0;
		while ((index < ranks.size()) && ((population.size() + ranks.get(index).size()) <= popSize)) {
			population.addAll(ranks.get(index));
			index++;
		}

		if (population.size() < popSize) {
			List<AggregationIndividual> rank = ranks.get(index);
			sortByCrowdingDistance(rank);
			while (population.size() < popSize) {
				population.add(rank.remove(0));
			}
		}
	}

	/**
	 * Sorts the given rank by crowding distance (i.e. the &quot;neighborhoodship&quot; in the
	 * fitness space).
	 */
	private void sortByCrowdingDistance(List<AggregationIndividual> rank) {
		Iterator<AggregationIndividual> f = rank.iterator();
		int numberOfCriteria = 0;
		while (f.hasNext()) {
			AggregationIndividual current = f.next();
			current.setCrowdingDistance(0.0d);
			numberOfCriteria = Math.max(numberOfCriteria, current.getPerformance().getSize());
		}

		for (int m = 0; m < numberOfCriteria; m++) {
			Comparator<AggregationIndividual> comparator = new CriteriaComparator(m);
			Collections.sort(rank, comparator);
			rank.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
			rank.get(rank.size() - 1).setCrowdingDistance(Double.POSITIVE_INFINITY);

			for (int i = 1; i < (rank.size() - 1); i++) {
				AggregationIndividual current = rank.get(i);
				double currentCrowdingDistance = current.getCrowdingDistance();
				AggregationIndividual afterI = rank.get(i + 1);
				AggregationIndividual beforeI = rank.get(i - 1);
				double afterPerformance = afterI.getPerformance().getCriterion(m).getFitness();
				double beforePerformance = beforeI.getPerformance().getCriterion(m).getFitness();
				current.setCrowdingDistance(currentCrowdingDistance + Math.abs(afterPerformance - beforePerformance));
			}
		}

		Collections.sort(rank, new CrowdingComparator());
	}

	/** Returns a list of non-dominated individuals. */
	private List<AggregationIndividual> getNextRank(List<AggregationIndividual> population) {
		List<AggregationIndividual> rank = new ArrayList<AggregationIndividual>();
		for (int i = 0; i < population.size(); i++) {
			AggregationIndividual current = population.get(i);
			rank.add(current);
			boolean delete = false;
			for (int j = rank.size() - 2; j >= 0; j--) {
				AggregationIndividual ranked = rank.get(j);
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
	public static boolean isDominated(AggregationIndividual i1, AggregationIndividual i2) {
		PerformanceVector pv1 = i1.getPerformance();
		PerformanceVector pv2 = i2.getPerformance();
		double[][] performances = new double[pv1.getSize()][2];
		for (int p = 0; p < performances.length; p++) {
			performances[p][0] = pv1.getCriterion(p).getFitness();
			performances[p][1] = pv2.getCriterion(p).getFitness();
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
