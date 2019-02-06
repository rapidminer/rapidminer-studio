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
package com.rapidminer.tools.math.optimization.ec.pso;

import com.rapidminer.operator.performance.PerformanceVector;


/**
 * This object stores for each individual index the current values and the best value for the
 * individual so far. It also stores the fitness of the best values ever and the corresponding
 * values.
 * 
 * @author Ingo Mierswa
 */
public class Population {

	private static final int CURRENT = 0;

	private static final int BEST = 1;

	private double[][][] values;

	private double[][] individualFitness;

	private double[] bestValuesEver;

	private PerformanceVector bestPerformanceEver;

	private double generationsBest = Double.NaN;

	private int generation = 0;

	private int lastImprovement = 0;

	public Population(int popSize, int individualSize) {
		values = new double[2][popSize][individualSize];
		individualFitness = new double[2][popSize];
		// setting best individual fitness to negative infinity
		for (int i = 0; i < popSize; i++) {
			individualFitness[BEST][i] = Double.NEGATIVE_INFINITY;
		}
	}

	public int getNumberOfIndividuals() {
		return values[CURRENT].length;
	}

	public int getIndividualSize() {
		return values[CURRENT][0].length;
	}

	public double[] getValues(int index) {
		return values[CURRENT][index];
	}

	public double[] getLocalBestValues(int index) {
		return values[BEST][index];
	}

	public double[] getGlobalBestValues() {
		return bestValuesEver;
	}

	public double getBestFitnessEver() {
		if (bestPerformanceEver == null) {
			return Double.NaN;
		} else {
			return bestPerformanceEver.getMainCriterion().getFitness();
		}
	}

	public PerformanceVector getBestPerformanceEver() {
		return bestPerformanceEver;
	}

	public double getBestFitnessInGeneration() {
		return generationsBest;
	}

	public void setValues(int index, double[] values) {
		this.values[CURRENT][index] = values;
	}

	public int getGeneration() {
		return generation;
	}

	public void nextGeneration() {
		generation++;
	}

	public int getGenerationsWithoutImprovement() {
		return (generation - lastImprovement);
	}

	public void setFitnessVector(PerformanceVector[] performanceVectors) {
		double[] fitnessValues = new double[performanceVectors.length];
		for (int i = 0; i < fitnessValues.length; i++) {
			fitnessValues[i] = performanceVectors[i] == null ? Double.NEGATIVE_INFINITY : performanceVectors[i]
					.getMainCriterion().getFitness();
		}
		this.generationsBest = Double.NEGATIVE_INFINITY;
		int generationsBestIndex = -1;
		for (int i = 0; i < fitnessValues.length; i++) {
			individualFitness[CURRENT][i] = fitnessValues[i];
			if (fitnessValues[i] > individualFitness[BEST][i]) {
				for (int d = 0; d < this.values[BEST][i].length; d++) {
					this.values[BEST][i][d] = this.values[CURRENT][i][d];
				}
				individualFitness[BEST][i] = fitnessValues[i];
			}
			if (fitnessValues[i] > generationsBest) {
				generationsBest = fitnessValues[i];
				generationsBestIndex = i;
			}
		}
		if ((bestPerformanceEver == null) || (generationsBest > bestPerformanceEver.getMainCriterion().getFitness())) {
			bestPerformanceEver = performanceVectors[generationsBestIndex];
			bestValuesEver = new double[values[CURRENT][0].length];
			for (int d = 0; d < bestValuesEver.length; d++) {
				bestValuesEver[d] = this.values[BEST][generationsBestIndex][d];
			}
			lastImprovement = generation;
		}
	}
}
