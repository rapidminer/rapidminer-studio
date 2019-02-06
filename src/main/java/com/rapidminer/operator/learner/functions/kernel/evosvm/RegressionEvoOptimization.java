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
package com.rapidminer.operator.learner.functions.kernel.evosvm;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.functions.kernel.SupportVector;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.kernels.Kernel;
import com.rapidminer.tools.math.optimization.ec.es.ESOptimization;
import com.rapidminer.tools.math.optimization.ec.es.Individual;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Evolutionary Strategy approach for SVM optimization. Currently only classification problems are
 * supported.
 * 
 * @author Ingo Mierswa
 */
public class RegressionEvoOptimization extends ESOptimization implements EvoOptimization {

	/** Number smaller than this number are regarded as zero. */
	// private final static double IS_ZERO = 1e-10;

	/** The training example set. */
	private ExampleSet exampleSet;

	/** The used kernel function. */
	private Kernel kernel;

	/** This parameter indicates the width of the regression tube loss function. */
	// private double epsilon = 0.0d;

	/** The label values. */
	private double[] ys;

	/** This function is to maximize. */
	private OptimizationFunction optimizationFunction;

	/** Creates a new evolutionary SVM optimization. */
	@Deprecated
	public RegressionEvoOptimization(
			ExampleSet exampleSet, // training data
			Kernel kernel, double c,
			double epsilon, // SVM paras
			int initType, // start population creation type para
			int maxIterations, int generationsWithoutImprovement,
			int popSize, // GA paras
			int selectionType, double tournamentFraction,
			boolean keepBest, // selection paras
			int mutationType, // type of mutation
			double crossoverProb, boolean showConvergencePlot, boolean showPopulationPlot, RandomGenerator random,
			LoggingHandler logging) {
		this(exampleSet, kernel, c, epsilon, initType, maxIterations, generationsWithoutImprovement, popSize, selectionType,
				tournamentFraction, keepBest, mutationType, crossoverProb, showConvergencePlot, showPopulationPlot, random,
				logging, null);
	}

	/** Creates a new evolutionary SVM optimization. */
	public RegressionEvoOptimization(
			ExampleSet exampleSet, // training data
			Kernel kernel, double c,
			double epsilon, // SVM paras
			int initType, // start population creation type para
			int maxIterations, int generationsWithoutImprovement,
			int popSize, // GA paras
			int selectionType, double tournamentFraction,
			boolean keepBest, // selection paras
			int mutationType, // type of mutation
			double crossoverProb, boolean showConvergencePlot, boolean showPopulationPlot, RandomGenerator random,
			LoggingHandler logging, Operator executingOperator) {

		super(EvoSVM.createBoundArray(0.0d, 2 * exampleSet.size()), EvoSVM.determineMax(c, kernel, exampleSet,
				selectionType, 2 * exampleSet.size()), popSize, 2 * exampleSet.size(), initType, maxIterations,
				generationsWithoutImprovement, selectionType, tournamentFraction, keepBest, mutationType, Double.NaN,
				crossoverProb, showConvergencePlot, showPopulationPlot, random, logging, executingOperator);

		this.exampleSet = exampleSet;
		this.kernel = kernel;
		// this.epsilon = epsilon;

		// label values
		this.ys = new double[exampleSet.size()];
		Iterator<Example> reader = exampleSet.iterator();
		int index = 0;
		while (reader.hasNext()) {
			Example example = reader.next();
			ys[index++] = example.getLabel();
		}

		// optimization function
		this.optimizationFunction = new RegressionOptimizationFunction(epsilon);
	}

	@Override
	public PerformanceVector evaluateIndividual(Individual individual) {
		double[] fitness = optimizationFunction.getFitness(individual.getValues(), ys, kernel);
		PerformanceVector performanceVector = new PerformanceVector();
		performanceVector.addCriterion(new EstimatedPerformance("SVM_fitness", fitness[0], 1, false));
		performanceVector.addCriterion(new EstimatedPerformance("SVM_complexity", fitness[1], 1, false));
		return performanceVector;
	}

	// ================================================================================
	// T R A I N
	// ================================================================================

	/**
	 * Trains the SVM. In this case an evolutionary strategy approach is applied to determine the
	 * best alpha values.
	 */
	@Override
	public EvoSVMModel train() throws OperatorException {
		optimize();
		return getModel(getBestValuesEver());
	}

	/** Delivers the fitness of the best individual as performance vector. */
	@Override
	public PerformanceVector getOptimizationPerformance() {
		double[] bestValuesEver = getBestValuesEver();
		double[] finalFitness = optimizationFunction.getFitness(bestValuesEver, ys, kernel);
		PerformanceVector result = new PerformanceVector();
		result.addCriterion(new EstimatedPerformance("svm_objective_function", finalFitness[0], 1, false));
		result.addCriterion(new EstimatedPerformance("no_support_vectors", -1 * finalFitness[1], 1, true));
		return result;
	}

	// ================================================================================
	// C R E A T E M O D E L
	// ================================================================================

	/**
	 * Returns a model containing all support vectors, i.e. the examples with non-zero alphas.
	 */
	private EvoSVMModel getModel(double[] alphas) {
		// calculate support vectors
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		Iterator<Example> reader = exampleSet.iterator();
		List<SupportVector> supportVectors = new ArrayList<>();
		int index = 0;
		while (reader.hasNext()) {
			double currentAlpha = alphas[index];
			Example currentExample = reader.next();
			if (currentAlpha != 0.0d) {
				double[] x = new double[regularAttributes.length];
				int a = 0;
				for (Attribute attribute : regularAttributes) {
					x[a++] = currentExample.getValue(attribute);
				}
				supportVectors.add(new SupportVector(x, ys[index], currentAlpha));
			}
			index++;
		}

		// calculate all sum values
		double[] sum = new double[exampleSet.size()];
		reader = exampleSet.iterator();
		index = 0;
		while (reader.hasNext()) {
			Example current = reader.next();
			double[] x = new double[regularAttributes.length];
			int a = 0;
			for (Attribute attribute : regularAttributes) {
				x[a++] = current.getValue(attribute);
			}
			sum[index] = kernel.getSum(supportVectors, x);
			index++;
		}

		// calculate b (from Stefan's mySVM code)
		double bSum = 0.0d;
		int bCounter = 1;
		/*
		 * int bCounter = 0; for (int i = 0; i < ys.length / 2; i++) { if ((ys[i] * alphas[i] - c <
		 * -IS_ZERO) && (ys[i] * alphas[i] > IS_ZERO)) { bSum += ys[i] - sum[i] - epsilon;
		 * bCounter++; } else if ((ys[i] * alphas[i] + c > IS_ZERO) && (ys[i] * alphas[i] <
		 * -IS_ZERO)) { bSum += ys[i] - sum[i] + epsilon; bCounter++; } }
		 * 
		 * if (bCounter == 0) { // unlikely bSum = 0.0d; for (int i = 0; i < ys.length / 2; i++) {
		 * if ((ys[i] * alphas[i] < IS_ZERO) && (ys[i] * alphas[i] > -IS_ZERO)) { bSum += ys[i] -
		 * sum[i] - epsilon; bCounter++; } } if (bCounter == 0) { // even unlikelier bSum = 0.0d;
		 * for (int i = 0; i < ys.length / 2; i++) { bSum += ys[i] - sum[i] - epsilon; bCounter++; }
		 * } }
		 */
		return new EvoSVMModel(exampleSet, supportVectors, kernel, bSum / bCounter);
	}
}
