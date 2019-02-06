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
package com.rapidminer.operator.learner.functions.kernel;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
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
 * Evolutionary Strategy approach for optimization of the logistic regression problem.
 * 
 * @author Ingo Mierswa
 */
public class KernelLogisticRegressionOptimization extends ESOptimization {

	/** Number smaller than this number are regarded as zero. */
	private final static double IS_ZERO = 1e-8;

	private ExampleSet exampleSet;

	private Kernel kernel;

	private double[] ys;

	private double c;

	private Attribute label;

	private Attribute weight;

	private boolean multiobjective = false;

	/** Creates a new evolutionary SVM optimization. */
	@Deprecated
	public KernelLogisticRegressionOptimization(ExampleSet exampleSet, // training data
			Kernel kernel, // kernel
			double c, int initType, // start population creation type para
			int maxIterations, int generationsWithoutImprovement, int popSize, // GA paras
			int selectionType, double tournamentFraction, boolean keepBest, // selection paras
			int mutationType, // type of mutation
			double crossoverProb, boolean showConvergencePlot, RandomGenerator random, LoggingHandler logging) {
		this(exampleSet, kernel, c, initType, maxIterations, generationsWithoutImprovement, popSize, selectionType,
				tournamentFraction, keepBest, mutationType, crossoverProb, showConvergencePlot, random, logging, null);
	}

	/** Creates a new evolutionary SVM optimization. */
	public KernelLogisticRegressionOptimization(
			ExampleSet exampleSet, // training data
			Kernel kernel, // kernel
			double c,
			int initType, // start population creation type para
			int maxIterations, int generationsWithoutImprovement,
			int popSize, // GA paras
			int selectionType, double tournamentFraction,
			boolean keepBest, // selection paras
			int mutationType, // type of mutation
			double crossoverProb, boolean showConvergencePlot, RandomGenerator random, LoggingHandler logging,
			Operator executingOperator) {

		super(0, 1.0, popSize, exampleSet.size(), initType, maxIterations, generationsWithoutImprovement, selectionType,
				tournamentFraction, keepBest, mutationType, crossoverProb, showConvergencePlot, false, random, logging,
				executingOperator);

		if (selectionType == NON_DOMINATED_SORTING_SELECTION) {
			multiobjective = true;
		} else {
			multiobjective = false;
		}

		this.exampleSet = exampleSet;
		this.kernel = kernel;
		this.kernel.init(this.exampleSet);
		this.c = c;
		this.label = exampleSet.getAttributes().getLabel();
		this.weight = exampleSet.getAttributes().getWeight();

		this.ys = new double[exampleSet.size()];
		int counter = 0;
		for (Example e : exampleSet) {
			ys[counter++] = e.getLabel() == label.getMapping().getPositiveIndex() ? 1 : 0;
		}
	}

	@Override
	public PerformanceVector evaluateIndividual(Individual individual) {
		double[] alphas = individual.getValues();

		double marginSum = 0.0d;
		for (int i = 0; i < ys.length; i++) {
			if (alphas[i] == 0.0d) {
				continue;
			}
			for (int j = 0; j < ys.length; j++) {
				if (alphas[j] == 0.0d) {
					continue;
				}
				marginSum += (alphas[i] * alphas[j] * ys[i] * ys[j] * kernel.getDistance(i, j));
			}
		}

		double errorSum = 0.0d;
		int i = 0;
		for (Example example : exampleSet) {
			double delta = alphas[i];
			if (!multiobjective) {
				delta /= c;
			}
			if (delta > 0.0d) {
				double weightValue = 1.0d;
				if (weight != null) {
					weightValue = example.getValue(weight);
				}
				// the following lines deliver the same results
				// double currentResult = weightValue * (ys[i] * Math.log(delta) + (1.0d - ys[i]) *
				// Math.log(1.0d - delta) + delta);
				double currentResult = weightValue * (delta * Math.log(delta) + (1.0d - delta) * Math.log(1.0d - delta));
				if (!Double.isNaN(currentResult)) {
					errorSum += currentResult;
				}
			}
			i++;
		}

		PerformanceVector performanceVector = new PerformanceVector();
		if (!multiobjective) {
			double fitness = marginSum + c * errorSum;
			performanceVector.addCriterion(new EstimatedPerformance("log_reg_fitness", fitness, 1, false));
		} else {
			performanceVector.addCriterion(new EstimatedPerformance("log_reg_margin", marginSum, 1, false));
			performanceVector.addCriterion(new EstimatedPerformance("log_reg_error", errorSum, 1, false));
		}
		return performanceVector;
	}

	public Model train() throws OperatorException {
		optimize();
		return getModel(getBestValuesEver());
	}

	/**
	 * Returns a model containing all support vectors, i.e. the examples with non-zero alphas.
	 */
	private Model getModel(double[] alphas) {
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
		int bCounter = 0;
		for (int i = 0; i < alphas.length; i++) {
			if ((ys[i] * alphas[i] - c < -IS_ZERO) && (ys[i] * alphas[i] > IS_ZERO)) {
				bSum += ys[i] - sum[i];
				bCounter++;
			} else if ((ys[i] * alphas[i] + c > IS_ZERO) && (ys[i] * alphas[i] < -IS_ZERO)) {
				bSum += ys[i] - sum[i];
				bCounter++;
			}
		}

		if (bCounter == 0) {
			// unlikely
			bSum = 0.0d;
			for (int i = 0; i < alphas.length; i++) {
				if ((ys[i] * alphas[i] < IS_ZERO) && (ys[i] * alphas[i] > -IS_ZERO)) {
					bSum += ys[i] - sum[i];
					bCounter++;
				}
			}
			if (bCounter == 0) {
				// even unlikelier
				bSum = 0.0d;
				for (int i = 0; i < alphas.length; i++) {
					bSum += ys[i] - sum[i];
					bCounter++;
				}
			}
		}
		return new KernelLogisticRegressionModel(exampleSet, supportVectors, kernel, bSum / bCounter);
	}
}
