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
package com.rapidminer.operator.learner.functions.linear;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.distribution.FDistribution;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * This implements an attribute selection method for linear regression that is based on a T-Test. It
 * will filter out all attributes whose coefficient is not significantly different from 0.
 *
 * @author Sebastian Land, Ingo Mierswa
 *
 */
public class TTestLinearRegressionMethod implements LinearRegressionMethod {

	public static final String PARAMETER_SIGNIFICANCE_LEVEL = "alpha";

	@Override
	public LinearRegressionResult applyMethod(LinearRegression regression, boolean useBias, double ridge,
			ExampleSet exampleSet, boolean[] isUsedAttribute, int numberOfExamples, int numberOfUsedAttributes,
			double[] means, double labelMean, double[] standardDeviations, double labelStandardDeviation,
			double[] coefficientsOnFullData, double errorOnFullData) throws UndefinedParameterError, ProcessStoppedException {
		double alpha = regression.getParameterAsDouble(PARAMETER_SIGNIFICANCE_LEVEL);

		LinearRegressionResult result = filterByPValue(regression, useBias, ridge, exampleSet, isUsedAttribute, means,
				labelMean, standardDeviations, labelStandardDeviation, coefficientsOnFullData, alpha);
		return result;
	}

	/**
	 * This method filters the selected attributes depending on their p-value in respect to the
	 * significance niveau alpha.
	 *
	 * @throws ProcessStoppedException
	 */
	protected LinearRegressionResult filterByPValue(LinearRegression regression, boolean useBias, double ridge,
			ExampleSet exampleSet, boolean[] isUsedAttribute, double[] means, double labelMean, double[] standardDeviations,
			double labelStandardDeviation, double[] coefficientsOnFullData, double alpha) throws UndefinedParameterError,
			ProcessStoppedException {

		FDistribution fdistribution;
		// check if the F-distribution can be calculated
		int secondDegreeOfFreedom = exampleSet.size() - coefficientsOnFullData.length;
		if (secondDegreeOfFreedom > 0) {
			fdistribution = new FDistribution(1, secondDegreeOfFreedom);
		} else {
			fdistribution = null;
		}

		double generalCorrelation = regression.getCorrelation(exampleSet, isUsedAttribute, coefficientsOnFullData, useBias);
		generalCorrelation *= generalCorrelation;

		int index = 0;
		for (int i = 0; i < isUsedAttribute.length; i++) {
			if (isUsedAttribute[i]) {
				double coefficient = coefficientsOnFullData[index];

				// only if it is possible to calculate the probabilities, the alpha value for this
				// attribute is checked
				if (fdistribution != null) {
					double probability = getPValue(coefficient, i, regression, useBias, ridge, exampleSet, isUsedAttribute,
							standardDeviations, labelStandardDeviation, fdistribution, generalCorrelation);
					if (1.0d - probability > alpha) {
						isUsedAttribute[i] = false;
					}
					index++;
				} else {
					isUsedAttribute[i] = false;
				}
			}
		}
		LinearRegressionResult result = new LinearRegressionResult();
		result.isUsedAttribute = isUsedAttribute;
		result.coefficients = regression.performRegression(exampleSet, isUsedAttribute, means, labelMean, ridge, useBias);
		result.error = regression.getSquaredError(exampleSet, isUsedAttribute, result.coefficients, useBias);
		return result;
	}

	/**
	 * Returns the PValue of the attributeIndex-th attribute that expresses the probability that the
	 * coefficient is only random.
	 *
	 * @throws ProcessStoppedException
	 */
	protected double getPValue(double coefficient, int attributeIndex, LinearRegression regression, boolean useBias,
			double ridge, ExampleSet exampleSet, boolean[] isUsedAttribute, double[] standardDeviations,
			double labelStandardDeviation, FDistribution fdistribution, double generalCorrelation)
			throws UndefinedParameterError, ProcessStoppedException {
		double tolerance = regression.getTolerance(exampleSet, isUsedAttribute, attributeIndex, ridge, useBias);
		double standardError = Math.sqrt((1.0d - generalCorrelation)
				/ (tolerance * (exampleSet.size() - exampleSet.getAttributes().size() - 1.0d)))
				* labelStandardDeviation / standardDeviations[attributeIndex];

		// calculating other statistics
		double tStatistics = coefficient / standardError;
		double probability = fdistribution.cumulativeProbability(tStatistics * tStatistics);
		return probability;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		LinkedList<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeDouble(PARAMETER_SIGNIFICANCE_LEVEL, "This is the significance level of the t-test.", 0,
				1, 0.05));
		return types;
	}

}
