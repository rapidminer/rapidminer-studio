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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.Tools;


/**
 * A set of individuals. Each individual contains the values vector and information about its
 * fitness.
 *
 * @author Ingo Mierswa
 */
public class Population {

	public static final Comparator<Individual> PERFORMANCE_COMPARATOR = new Comparator<Individual>() {

		@Override
		public int compare(Individual i1, Individual i2) {
			PerformanceVector p1 = i1.getFitness();
			PerformanceVector p2 = i2.getFitness();
			return p1.compareTo(p2);
		}
	};

	/** List of individuals. */
	private ArrayList<Individual> individuals = new ArrayList<Individual>();

	/** All generations' best individual. */
	private Individual bestEver;

	/** The currently best individual. */
	private Individual currentBest;

	/** The number of generations. */
	private int generations = 1;

	/** The last generation where setBestEver() was invoked. */
	private int lastImprovement = 1;

	/** Removes all individuals. */
	public void clear() {
		individuals.clear();
	}

	/** Adds a single individual. */
	public void add(Individual individual) {
		individuals.add(individual);
	}

	/** Adds all individuals from the given collection. */
	public void addAll(Collection<Individual> newIndividuals) {
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

	public void setCurrentBest(Individual currentBest) {
		this.currentBest = currentBest;
	}

	public Individual getCurrentBest() {
		return this.currentBest;
	}

	public void setBestEver(Individual bestEver) {
		this.lastImprovement = generations;
		this.bestEver = bestEver;
	}

	public Individual getBestEver() {
		return bestEver;
	}

	public void nextGeneration() {
		this.generations++;
	}

	public int getGeneration() {
		return generations;
	}

	public int getGenerationsWithoutImprovement() {
		return generations - lastImprovement;
	}

	/**
	 * Sorts the individuals in ascending order according to their performance, thus the best one
	 * will be in last position.
	 */
	public void sort() {
		Collections.sort(individuals, PERFORMANCE_COMPARATOR);
	}

	/**
	 * Sorts the individuals in ascending order according to their performance, thus the best one
	 * will be in last position.
	 */
	public void sort(Comparator<Individual> comparator) {
		Collections.sort(individuals, comparator);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("Population in generation " + generations + ":" + Tools.getLineSeparator());
		Iterator<Individual> i = individuals.iterator();
		while (i.hasNext()) {
			result.append(i.next() + Tools.getLineSeparator());
		}
		return result.toString();
	}
}
