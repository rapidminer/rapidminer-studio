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

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.optimization.ec.es.ESOptimization;
import com.rapidminer.tools.math.optimization.ec.es.Individual;
import com.rapidminer.tools.math.optimization.ec.es.OptimizationValueType;


/**
 * <p>
 * This regression learning operator fits a polynomial of all attributes to the given data set. If
 * the data set contains a label Y and three attributes X1, X2, and X3 a function of the form<br />
 * <br />
 * <code>Y = w0 + w1 * X1 ^ d1 + w2 * X2 ^ d2 + w3 * X3 ^ d3</code><br />
 * <br />
 * will be fitted to the training data.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class PolynomialRegression extends AbstractLearner {

	public static final String PARAMETER_MAX_ITERATIONS = "max_iterations";

	public static final String PARAMETER_REPLICATION_FACTOR = "replication_factor";

	public static final String PARAMETER_MAX_DEGREE = "max_degree";

	public static final String PARAMETER_MIN_COEFFICIENT = "min_coefficient";

	public static final String PARAMETER_MAX_COEFFICIENT = "max_coefficient";

	private static class RegressionOptimization extends ESOptimization {

		private int replicationFactor;

		private ExampleSet exampleSet;

		private Attribute label;

		private Attribute[] attributes;

		public RegressionOptimization(ExampleSet exampleSet, int replicationFactor, int maxIterations, int maxDegree,
				double minCoefficient, double maxCoefficient, RandomGenerator random, LoggingHandler logging,
				Operator executingOperator) {

			super(getMinVector(exampleSet, replicationFactor, minCoefficient),
					getMaxVector(exampleSet, replicationFactor, maxDegree, maxCoefficient), 1,
					exampleSet.getAttributes().size() * 2 * replicationFactor + 1, ESOptimization.INIT_TYPE_RANDOM,
					maxIterations, maxIterations, ESOptimization.TOURNAMENT_SELECTION, 1.0, true,
					ESOptimization.GAUSSIAN_MUTATION, 0.01d, 0.0d, false, false, random, logging, executingOperator);

			this.replicationFactor = replicationFactor;
			this.exampleSet = exampleSet;
			this.label = exampleSet.getAttributes().getLabel();
			this.attributes = new Attribute[exampleSet.getAttributes().size()];
			int i = 0;
			for (Attribute attribute : exampleSet.getAttributes()) {
				attributes[i] = attribute;
				i++;
			}

			int index = 0;
			for (int a = 0; a < exampleSet.getAttributes().size(); a++) {
				for (int f = 0; f < replicationFactor; f++) {
					setValueType(index++, OptimizationValueType.VALUE_TYPE_DOUBLE);
					setValueType(index++, OptimizationValueType.VALUE_TYPE_INT);
				}
			}
			setValueType(exampleSet.getAttributes().size() * replicationFactor * 2, OptimizationValueType.VALUE_TYPE_DOUBLE);
		}

		private static double[] getMinVector(ExampleSet exampleSet, int replicationFactor, double minCoefficient) {
			double[] result = new double[exampleSet.getAttributes().size() * replicationFactor * 2 + 1];
			int index = 0;
			for (int a = 0; a < exampleSet.getAttributes().size(); a++) {
				for (int f = 0; f < replicationFactor; f++) {
					result[index++] = minCoefficient;
					result[index++] = 1;
				}
			}
			result[result.length - 1] = minCoefficient;
			return result;
		}

		private static double[] getMaxVector(ExampleSet exampleSet, int replicationFactor, double maxDegree,
				double maxCoefficient) {
			double[] result = new double[exampleSet.getAttributes().size() * replicationFactor * 2 + 1];
			int index = 0;
			for (int a = 0; a < exampleSet.getAttributes().size(); a++) {
				for (int f = 0; f < replicationFactor; f++) {
					result[index++] = maxCoefficient;
					result[index++] = maxDegree;
				}
			}
			result[result.length - 1] = maxCoefficient;
			return result;
		}

		@Override
		public PerformanceVector evaluateIndividual(Individual individual) throws OperatorException {
			double[] values = individual.getValues();
			double[][] coefficients = getCoefficients(values);
			double[][] degrees = getDegrees(values);
			double offset = getOffset(values);

			double error = 0.0d;
			for (Example example : exampleSet) {
				double prediction = PolynomialRegressionModel.calculatePrediction(example, attributes, coefficients, degrees,
						offset);
				double diff = Math.abs(example.getValue(label) - prediction);
				error += diff * diff;
			}
			error = Math.sqrt(error);

			PerformanceVector performanceVector = new PerformanceVector();
			performanceVector.addCriterion(new EstimatedPerformance("Polynomial Regression Error", error, 1, true));
			return performanceVector;
		}

		public double[][] getCoefficients(double[] values) {
			int attSize = exampleSet.getAttributes().size();
			double[][] coefficients = new double[replicationFactor][attSize];
			for (int f = 0; f < replicationFactor; f++) {
				for (int a = 0; a < attSize; a++) {
					coefficients[f][a] = values[(f * attSize * 2) + a * 2];
				}
			}
			return coefficients;
		}

		public double[][] getDegrees(double[] values) {
			int attSize = exampleSet.getAttributes().size();
			double[][] degrees = new double[replicationFactor][attSize];
			for (int f = 0; f < replicationFactor; f++) {
				for (int a = 0; a < attSize; a++) {
					degrees[f][a] = values[(f * attSize * 2) + a * 2 + 1];
				}
			}
			return degrees;
		}

		public double getOffset(double[] values) {
			return values[values.length - 1];
		}
	}

	public PolynomialRegression(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		RegressionOptimization optimization = new RegressionOptimization(exampleSet,
				getParameterAsInt(PARAMETER_REPLICATION_FACTOR), getParameterAsInt(PARAMETER_MAX_ITERATIONS),
				getParameterAsInt(PARAMETER_MAX_DEGREE), getParameterAsDouble(PARAMETER_MIN_COEFFICIENT),
				getParameterAsDouble(PARAMETER_MAX_COEFFICIENT), RandomGenerator.getRandomGenerator(this), this, this);

		optimization.optimize();
		double[] values = optimization.getBestValuesEver();

		double[][] coefficients = optimization.getCoefficients(values);
		double[][] degrees = optimization.getDegrees(values);
		double offset = optimization.getOffset(values);

		this.checkForStop();
		return new PolynomialRegressionModel(exampleSet, coefficients, degrees, offset);
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return PolynomialRegressionModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc.equals(OperatorCapability.NUMERICAL_ATTRIBUTES)) {
			return true;
		}
		if (lc.equals(OperatorCapability.NUMERICAL_LABEL)) {
			return true;
		}
		if (lc == OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_MAX_ITERATIONS,
				"The maximum number of iterations used for model fitting.", 1, Integer.MAX_VALUE, 5000);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(
				PARAMETER_REPLICATION_FACTOR,
				"The amount of times each input variable is replicated, i.e. how many different degrees and coefficients can be applied to each variable",
				1, Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MAX_DEGREE, "The maximal degree used for the final polynomial.", 1,
				Integer.MAX_VALUE, 5);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MIN_COEFFICIENT,
				"The minimum number used for the coefficients and the offset.", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, -100);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MAX_COEFFICIENT,
				"The maximum number used for the coefficients and the offset.", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 100);
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
