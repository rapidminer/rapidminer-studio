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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.optimization.Optimization;


/**
 * This class performs the optimization of a value vector with a particle swarm approach.
 *
 * @author Ingo Mierswa
 */
public abstract class PSOOptimization implements Optimization {

	private int maxGen;

	private int maxWithoutImprovement;

	private double inertiaWeight;

	private double localWeight;

	private double globalWeight;

	private double minValue;

	private double maxValue;

	private double inertiaWeightDelta;

	private RandomGenerator random;

	private Population population;

	private Operator operator;

	/**
	 * Creates a new PSO optimization with the given parameters.
	 *
	 * @deprecated Please use
	 *             {@link #PSOOptimization(int popSize, int individualSize, int maxGen, int maxWithoutImprovement, double inertiaWeight, double localWeight, double globalWeight, double minValue, double maxValue, boolean dynamicInertiaWeight, RandomGenerator random, Operator op)}
	 *             instead. Creating the PSOOptimization without an operator will not display the
	 *             progress
	 */
	@Deprecated
	public PSOOptimization(int popSize, int individualSize, int maxGen, int maxWithoutImprovement, double inertiaWeight,
			double localWeight, double globalWeight, double minValue, double maxValue, boolean dynamicInertiaWeight,
			RandomGenerator random) {
		this.maxGen = maxGen;
		this.maxWithoutImprovement = maxWithoutImprovement;
		if (this.maxWithoutImprovement < 1) {
			this.maxWithoutImprovement = this.maxGen;
		}
		this.inertiaWeight = inertiaWeight;
		this.localWeight = localWeight;
		this.globalWeight = globalWeight;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.inertiaWeightDelta = 0.0d;
		if (dynamicInertiaWeight) {
			inertiaWeightDelta = inertiaWeight / maxGen;
		}
		this.random = random;
		this.population = createInitialPopulation(popSize, individualSize);
	}

	/**
	 * Creates a new PSO optimization with the given parameters.
	 */
	public PSOOptimization(int popSize, int individualSize, int maxGen, int maxWithoutImprovement, double inertiaWeight,
			double localWeight, double globalWeight, double minValue, double maxValue, boolean dynamicInertiaWeight,
			RandomGenerator random, Operator op) {
		this(popSize, individualSize, maxGen, maxWithoutImprovement, inertiaWeight, localWeight, globalWeight, minValue,
				maxValue, dynamicInertiaWeight, random);
		this.operator = op;
	}

	/**
	 * Subclasses must implement this method to calculate the fitness of the given individual.
	 * Please note that null might be returned for non-valid individuals.
	 */
	public abstract PerformanceVector evaluateIndividual(double[] individual) throws OperatorException;

	/**
	 * This method is invoked after each evaluation. The default implementation does nothing but
	 * subclasses might implement this method to support online plotting or logging.
	 */
	public void nextIteration() throws OperatorException {}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	/** Creates the initial population. */
	protected Population createInitialPopulation(int popSize, int individualSize) {
		Population initPop = new Population(popSize, individualSize);
		for (int i = 0; i < popSize; i++) {
			double[] values = new double[individualSize];
			for (int j = 0; j < values.length; j++) {
				values[j] = random.nextDoubleInRange(minValue, maxValue);
			}
			initPop.setValues(i, values);
		}
		return initPop;
	}

	public void reinit(int popSize, int individualSize, int maxGen, int maxWithoutImprovement, double inertiaWeight,
			double localWeight, double globalWeight, double minValue, double maxValue, boolean dynamicInertiaWeight,
			RandomGenerator random) {
		this.maxGen = maxGen;
		this.maxWithoutImprovement = maxWithoutImprovement;
		if (this.maxWithoutImprovement < 1) {
			this.maxWithoutImprovement = this.maxGen;
		}
		this.inertiaWeight = inertiaWeight;
		this.localWeight = localWeight;
		this.globalWeight = globalWeight;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.inertiaWeightDelta = 0.0d;
		if (dynamicInertiaWeight) {
			inertiaWeightDelta = inertiaWeight / maxGen;
		}
		this.random = random;
		this.population = createInitialPopulation(popSize, individualSize);
	}

	/** Invoke this method for optimization. */
	@Override
	public void optimize() throws OperatorException {
		// velocities
		double[][] velocities = new double[population.getNumberOfIndividuals()][population.getIndividualSize()];
		for (int p = 0; p < velocities.length; p++) {
			for (int a = 0; a < velocities[p].length; a++) {
				velocities[p][a] = 0.0d;
			}
		}

		evaluate(population);
		population.nextGeneration();
		nextIteration();
		if (operator != null) {
			operator.getProgress().setTotal(maxGen);
		}
		while (!(population.getGeneration() >= maxGen
				|| population.getGenerationsWithoutImprovement() >= maxWithoutImprovement || population.getBestFitnessEver() >= population
				.getBestPerformanceEver().getMainCriterion().getMaxFitness())) {
			if (operator != null) {
				operator.getProgress().setCompleted(population.getGeneration());
			}

			// update velocities
			double[] globalBest = population.getGlobalBestValues();
			for (int i = 0; i < velocities.length; i++) {
				double[] localBest = population.getLocalBestValues(i);
				double[] current = population.getValues(i);
				for (int d = 0; d < velocities[i].length; d++) {
					velocities[i][d] = inertiaWeight * velocities[i][d] + localWeight * (random.nextGaussian() + 1.0d)
							* (localBest[d] - current[d]) + globalWeight * (random.nextGaussian() + 1.0d)
							* (globalBest[d] - current[d]);
				}
			}
			// update positions
			for (int i = 0; i < velocities.length; i++) {
				double[] current = population.getValues(i);
				double[] newValues = new double[current.length];
				for (int d = 0; d < velocities[i].length; d++) {
					newValues[d] = current[d] + velocities[i][d];
					if (newValues[d] < minValue) {
						newValues[d] = minValue;
					}
					if (newValues[d] > maxValue) {
						newValues[d] = maxValue;
					}
				}
				population.setValues(i, newValues);
			}
			inertiaWeight -= inertiaWeightDelta;
			evaluate(population);
			population.nextGeneration();
			nextIteration();
		}
	}

	/**
	 * Calculates the fitness for all individuals and gives the fitness values to the population.
	 */
	private void evaluate(Population population) throws OperatorException {
		PerformanceVector[] fitnessValues = new PerformanceVector[population.getNumberOfIndividuals()];
		for (int i = 0; i < fitnessValues.length; i++) {
			double[] individual = population.getValues(i);
			fitnessValues[i] = evaluateIndividual(individual);
		}
		population.setFitnessVector(fitnessValues);
	}

	/** Returns the current generation. */
	@Override
	public int getGeneration() {
		return population.getGeneration();
	}

	/** Returns the best fitness in the current generation. */
	@Override
	public double getBestFitnessInGeneration() {
		return population.getBestFitnessInGeneration();
	}

	/** Returns the best fitness ever. */
	@Override
	public double getBestFitnessEver() {
		return population.getBestFitnessEver();
	}

	/** Returns the best performance vector ever. */
	@Override
	public PerformanceVector getBestPerformanceEver() {
		return population.getBestPerformanceEver();
	}

	/**
	 * Returns the best values ever. Use this method after optimization to get the best result.
	 */
	@Override
	public double[] getBestValuesEver() {
		return population.getGlobalBestValues();
	}
}
