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
package com.rapidminer.operator.features;

import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;


/**
 * A set of individuals. Stores generation number and best individuals.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class Population {

	public static final Comparator<Individual> PERFORMANCE_COMPARATOR = new Comparator<Individual>() {

		@Override
		public int compare(Individual i1, Individual i2) {
			PerformanceVector p1 = i1.getPerformance();
			PerformanceVector p2 = i2.getPerformance();
			if (p1 == null) {
				if (p2 == null) {
					return 0;
				}
				return -1;
			} else {
				if (p2 == null) {
					return 1;
				} else {
					return p1.compareTo(p2);
				}
			}
		}
	};

	/** List of ExampleSet */
	private ArrayList<Individual> individuals = new ArrayList<Individual>();

	/** Current generation number */
	private int generation = 0;

	/** Generation of the last improval. */
	private int generationOfLastImproval = 0;

	/** All generations' best individual. */
	private Individual bestEver;

	/** All generations' best performance. */
	private PerformanceVector bestPerformanceEver;

	/** The currently best individual. */
	private Individual currentBest;

	/** The currently best performance. */
	private PerformanceVector currentBestPerformance;

	/** Construct an empty generation. */
	public Population() {}

	/** Removes all individuals. */
	public void clear() {
		individuals.clear();
	}

	/** Adds a single individual. */
	public void add(Individual individual) {
		individuals.add(individual);
	}

	/** Adds all individuals from the given collection. */
	public void addAllIndividuals(Collection<Individual> newIndividuals) {
		individuals.addAll(newIndividuals);
	}

	/** Removes a single individual. */
	public void remove(Individual individual) {
		individuals.remove(individual);
	}

	/** Removes a single individual. */
	public void remove(int i) {
		individuals.remove(i);
	}

	/** Returns a single individual. */
	public Individual get(int i) {
		return individuals.get(i);
	}

	/** Returns the number of all individuals. */
	public int getNumberOfIndividuals() {
		return individuals.size();
	}

	/** Returns true is the population contains no individuals. */
	public boolean empty() {
		return individuals.size() == 0;
	}

	/** Increase the generation number by one. */
	public void nextGeneration() {
		generation++;
	}

	/** Returns the current number of the generation. */
	public int getGeneration() {
		return generation;
	}

	/**
	 * Returns the number of generations without improval.
	 */
	public int getGenerationsWithoutImproval() {
		return generation - generationOfLastImproval;
	}

	/**
	 * Remember the current generation's best individual and update the best individual.
	 */
	public void updateEvaluation() {
		currentBest = searchBest();
		currentBestPerformance = (currentBest == null) ? null : currentBest.getPerformance();
		if ((bestEver == null) || ((currentBest != null) && (currentBestPerformance.compareTo(bestPerformanceEver) > 0))) {
			bestEver = new Individual(currentBest.getWeights());
			bestEver.setPerformance(currentBest.getPerformance());
			bestPerformanceEver = bestEver.getPerformance();
			generationOfLastImproval = generation;
		}
	}

	/**
	 * Finds the current generation's best individual. Returns null, if there are unevaluated
	 * individuals. Probably you will want to use <tt>bestEver()</tt> or <tt>lastBest()</tt> because
	 * they don't cause comparisons.
	 */
	private Individual searchBest() {
		try {
			return Collections.max(individuals, PERFORMANCE_COMPARATOR);
		} catch (NullPointerException e) {
			return null;
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	/** Returns the best performance. */
	public PerformanceVector getBestPerformanceEver() {
		return bestPerformanceEver;
	}

	/** Returns all generations' best individual. */
	public Individual getBestIndividualEver() {
		return bestEver;
	}

	/** Returns the last generation's best individual. */
	public Individual getCurrentBest() {
		return currentBest;
	}

	/** Returns the last generation's best performance. */
	public PerformanceVector getCurrentBestPerformance() {
		return currentBestPerformance;
	}

	/**
	 * Sorts the individuals in ascending order according to their performance, thus the best one
	 * will be in last position.
	 */
	public void sort() {
		Collections.sort(individuals, PERFORMANCE_COMPARATOR);
	}

	@Override
	public String toString() {
		StringBuffer s = new StringBuffer("Generation: " + generation + ",size: " + getNumberOfIndividuals()
				+ ", individuals:" + Tools.getLineSeparator());
		for (int i = 0; i < getNumberOfIndividuals(); i++) {
			if (i > 0) {
				s.append(",");
			}
			s.append(i + ": " + Arrays.toString(get(i).getWeights()) + Tools.getLineSeparator());
		}
		s.append("]");
		return s.toString();
	}
}
