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
package com.rapidminer.operator.learner.functions;

import Jama.Matrix;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.optimization.ec.es.ESOptimization;
import com.rapidminer.tools.math.optimization.ec.es.Individual;


/**
 * Evolutionary Strategy approach for optimization of the logistic regression problem.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class LogisticRegressionOptimization extends ESOptimization {

	private ExampleSet exampleSet;

	private Attribute label;

	private Attribute weight;

	private boolean addIntercept;

	LoggingHandler logging;

	/** Creates a new evolutionary optimization. */
	@Deprecated
	public LogisticRegressionOptimization(ExampleSet exampleSet, // training data
			boolean addIntercept, // add intercept
			int initType, // start population creation type para
			int maxIterations, int generationsWithoutImprovement, int popSize, // GA paras
			int selectionType, double tournamentFraction, boolean keepBest, // selection paras
			int mutationType, // type of mutation
			double crossoverProb, boolean showConvergencePlot, RandomGenerator random, LoggingHandler logging) {
		this(exampleSet, addIntercept, initType, maxIterations, generationsWithoutImprovement, popSize, selectionType,
				tournamentFraction, keepBest, mutationType, crossoverProb, showConvergencePlot, random, logging, null);
	}

	/** Creates a new evolutionary optimization. */
	public LogisticRegressionOptimization(
			ExampleSet exampleSet, // training data
			boolean addIntercept, // add intercept
			int initType, // start population creation type para
			int maxIterations, int generationsWithoutImprovement,
			int popSize, // GA paras
			int selectionType, double tournamentFraction,
			boolean keepBest, // selection paras
			int mutationType, // type of mutation
			double crossoverProb, boolean showConvergencePlot, RandomGenerator random, LoggingHandler logging,
			Operator executingOperator) {

		super(-1.0d, 1.0d, popSize,
				addIntercept ? exampleSet.getAttributes().size() + 1 : exampleSet.getAttributes().size(), initType,
				maxIterations, generationsWithoutImprovement, selectionType, tournamentFraction, keepBest, mutationType,
				crossoverProb, showConvergencePlot, false, random, logging, executingOperator);

		this.logging = logging;

		this.exampleSet = exampleSet;
		this.label = exampleSet.getAttributes().getLabel();
		this.weight = exampleSet.getAttributes().getWeight();

		this.addIntercept = addIntercept;
	}

	@Override
	public PerformanceVector evaluateIndividual(Individual individual) {
		double[] beta = individual.getValues();

		double fitness = 0.0d;
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Example example : exampleSet) {
			double eta = 0.0d;
			int i = 0;
			for (Attribute attribute : regularAttributes) {
				double value = example.getValue(attribute);
				eta += beta[i] * value;
				i++;
			}
			if (addIntercept) {
				eta += beta[beta.length - 1];
			}
			double pi = Math.exp(eta) / (1 + Math.exp(eta));

			double classValue = example.getValue(label);
			double currentFitness = classValue * Math.log(pi) + (1 - classValue) * Math.log(1 - pi);
			double weightValue = 1.0d;
			if (weight != null) {
				weightValue = example.getValue(weight);
			}
			fitness += weightValue * currentFitness;
		}

		PerformanceVector performanceVector = new PerformanceVector();
		performanceVector.addCriterion(new EstimatedPerformance("log_reg_fitness", fitness, exampleSet.size(), false));
		return performanceVector;
	}

	public LogisticRegressionModel train() throws OperatorException {
		optimize();
		return new LogisticRegressionModel(this.exampleSet, getBestValuesEver(), estimateVariance(), addIntercept);
	}

	private double[] estimateVariance() {
		double[] beta = getBestValuesEver();

		Matrix hessian = new Matrix(beta.length, beta.length);
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Example example : exampleSet) {
			double[] values = new double[beta.length];
			double eta = 0.0d;
			int j = 0;
			for (Attribute attribute : regularAttributes) {
				double value = example.getValue(attribute);
				values[j] = value;
				eta += beta[j] * value;
				j++;
			}
			if (addIntercept) {
				values[beta.length - 1] = 1.0d;
				eta += beta[beta.length - 1];
			}
			double pi = Math.exp(eta) / (1 + Math.exp(eta));

			double weightValue = 1.0d;
			if (weight != null) {
				weightValue = example.getValue(weight);
			}
			for (int x = 0; x < beta.length; x++) {
				for (int y = 0; y < beta.length; y++) {
					// sum is second derivative of log likelihood function
					double h = hessian.get(x, y) - values[x] * values[y] * weightValue * pi * (1 - pi);
					hessian.set(x, y, h);
				}
			}
		}

		double[] variance = new double[beta.length];
		Matrix varianceCovarianceMatrix = null;
		try {
			// asymptotic variance-covariance matrix is inverse of hessian matrix
			varianceCovarianceMatrix = hessian.inverse();
		} catch (Exception e) {
			logging.logWarning("could not determine variance-covariance matrix, hessian is singular");
			for (int j = 0; j < beta.length; j++) {
				variance[j] = Double.NaN;
			}
			return variance;
		}
		for (int j = 0; j < beta.length; j++) {
			// get diagonal elements
			variance[j] = Math.abs(varianceCovarianceMatrix.get(j, j));
		}

		return variance;
	}

	@Override
	public void nextIteration() {
		double[] beta = getBestValuesEver();
		for (int i = 0; i < beta.length; i++) {
			double minBound = getMin(i);
			double maxBound = getMax(i);
			if (beta[i] == minBound) {
				minBound -= 1;
				setMin(i, minBound);
				logging.log("decrease lower bound for beta(" + i + ") to + " + minBound);
			}
			if (beta[i] == maxBound) {
				maxBound += 1;
				setMax(i, maxBound);
				logging.log("increase upper bound for beta(" + i + ") to + " + maxBound);
			}
		}
	}

	public PerformanceVector getPerformance() {
		double[] beta = getBestValuesEver();
		double numberOfSlopes = addIntercept ? beta.length - 1 : beta.length;
		double logLikelihood = getBestFitnessEver();
		double restrictedLogLikelihood = 0.0d;
		double minusTwoLogLikelihood = 0.0d;
		double modelChiSquared = 0.0d;
		double goodnessOfFit = 0.0d;
		double coxSnellRSquared = 0.0d;
		double nagelkerkeRSquared = 0.0d;
		double mcfaddenRSquared = 0.0d;
		double AIC = 0.0d;
		double BIC = 0.0d;

		double weightSum = 0.0d;
		double positiveSum = 0.0d;
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Example example : exampleSet) {
			double eta = 0.0d;
			int i = 0;
			for (Attribute attribute : regularAttributes) {
				double value = example.getValue(attribute);
				eta += beta[i] * value;
				i++;
			}
			if (addIntercept) {
				eta += beta[beta.length - 1];
			}
			double pi = Math.exp(eta) / (1 + Math.exp(eta));

			double classValue = example.getValue(label);
			double currentFit = (classValue - pi) * (classValue - pi) / (pi * (1 - pi));
			double weightValue = 1.0d;
			if (weight != null) {
				weightValue = example.getValue(weight);
			}
			weightSum += weightValue;
			positiveSum += weightValue * classValue;
			goodnessOfFit += weightValue * currentFit;
		}
		double pi0 = positiveSum / weightSum;
		if (addIntercept) {
			restrictedLogLikelihood = weightSum * (pi0 * Math.log(pi0) + (1 - pi0) * Math.log(1 - pi0));
		} else {
			restrictedLogLikelihood = weightSum * Math.log(0.5);
		}
		minusTwoLogLikelihood = -2 * logLikelihood;
		modelChiSquared = 2 * (logLikelihood - restrictedLogLikelihood);
		coxSnellRSquared = 1 - Math.pow(Math.exp(restrictedLogLikelihood) / Math.exp(logLikelihood), 2 / weightSum);
		nagelkerkeRSquared = coxSnellRSquared / (1 - Math.pow(Math.exp(restrictedLogLikelihood), 2 / weightSum));
		mcfaddenRSquared = 1 - logLikelihood / restrictedLogLikelihood;
		AIC = -2 * logLikelihood + 2 * (numberOfSlopes + 1);
		BIC = -2 * logLikelihood + Math.log(weightSum) * (numberOfSlopes + 1);

		PerformanceVector estimatedPerformance = new PerformanceVector();
		estimatedPerformance
				.addCriterion(new EstimatedPerformance("log_likelihood", logLikelihood, exampleSet.size(), false));
		estimatedPerformance.addCriterion(new EstimatedPerformance("restricted_log_likelihood", restrictedLogLikelihood,
				exampleSet.size(), false));
		estimatedPerformance.addCriterion(new EstimatedPerformance("-2_log_likelihood", minusTwoLogLikelihood, exampleSet
				.size(), true));
		estimatedPerformance.addCriterion(new EstimatedPerformance("model_chi_squared", modelChiSquared, exampleSet.size(),
				false));
		estimatedPerformance.addCriterion(new EstimatedPerformance("goodness_of_fit", goodnessOfFit, exampleSet.size(),
				false));
		estimatedPerformance.addCriterion(new EstimatedPerformance("cox_snell_r_squared", coxSnellRSquared, exampleSet
				.size(), false));
		estimatedPerformance.addCriterion(new EstimatedPerformance("nagelkerke_r_squared", nagelkerkeRSquared, exampleSet
				.size(), false));
		estimatedPerformance.addCriterion(new EstimatedPerformance("mcfadden_r_squared", mcfaddenRSquared,
				exampleSet.size(), false));
		estimatedPerformance.addCriterion(new EstimatedPerformance("AIC", AIC, exampleSet.size(), true));
		estimatedPerformance.addCriterion(new EstimatedPerformance("BIC", BIC, exampleSet.size(), true));
		estimatedPerformance.setMainCriterionName("AIC");
		return estimatedPerformance;
	}
}
